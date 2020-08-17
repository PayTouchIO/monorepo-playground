package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.clients.urbanairship.entities.Pass
import io.paytouch.core.conversions.LoyaltyMembershipConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.errors._
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.filters.LoyaltyMembershipFilter
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.LoyaltyProgramValidator

class LoyaltyMembershipService(
    barcodeService: BarcodeService,
    customerMerchantService: => CustomerMerchantService,
    val customerLocationService: CustomerLocationService,
    loyaltyProgramService: => LoyaltyProgramService,
    loyaltyPointsHistoryService: LoyaltyPointsHistoryService,
    val merchantService: MerchantService,
    val messageHandler: SQSMessageHandler,
    orderService: => OrderService,
    val passService: PassService,
    val urbanAirshipService: UrbanAirshipService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LoyaltyMembershipConversions
       with LazyLogging {

  protected val dao = daos.loyaltyMembershipDao
  val loyaltyProgramDao = daos.loyaltyProgramDao
  val defaultFilter = LoyaltyMembershipFilter()

  val loyaltyProgramValidator = new LoyaltyProgramValidator

  type Record = LoyaltyMembershipRecord
  type Entity = LoyaltyMembership

  def enrich(record: Record)(implicit merchant: MerchantContext): Future[Entity] =
    enrich(Seq(record)).map(_.head)

  def enrich(records: Seq[Record])(implicit merchant: MerchantContext) = {
    val customerTotalsByCustomerR = getCustomerTotalsByCustomer(records)
    val loyaltyProgramByCustomerR = getLoyaltyProgramByCustomer(records)
    for {
      customerTotalsByCustomer <- customerTotalsByCustomerR
      loyaltyProgramByCustomer <- loyaltyProgramByCustomerR
    } yield fromRecordsToEntities(records, customerTotalsByCustomer, loyaltyProgramByCustomer)
  }

  private def getCustomerTotalsByCustomer(
      records: Seq[Record],
    )(implicit
      merchant: MerchantContext,
    ): Future[Map[Record, CustomerTotals]] = {
    val customerIds = records.map(_.customerId)
    customerLocationService
      .getTotalsPerCustomerForMerchant(customerIds, locationId = None)
      .map(_.mapKeysToObjs(records)(_.customerId))
  }

  private def getLoyaltyProgramByCustomer(
      records: Seq[Record],
    )(implicit
      merchant: MerchantContext,
    ): Future[Map[Record, LoyaltyProgram]] = {
    val loyaltyProgramIds = records.map(_.loyaltyProgramId)
    loyaltyProgramService
      .findByIds(loyaltyProgramIds)
      .map { loyaltyPrograms =>
        records.flatMap { record =>
          loyaltyPrograms.find(_.id == record.loyaltyProgramId).map(loyaltyProgram => record -> loyaltyProgram)
        }.toMap
      }
  }

  def findByLookupId(lookupId: String)(implicit user: UserContext): Future[Option[Entity]] = {
    implicit val merchant = user.toMerchantContext
    dao.findByLookupId(lookupId)
  }

  def findById(id: UUID)(implicit merchant: MerchantContext): Future[Option[Entity]] =
    findByIds(Seq(id)).map(_.headOption)

  def findByIds(ids: Seq[UUID])(implicit merchant: MerchantContext): Future[Seq[Entity]] =
    dao.findByIds(ids).flatMap(enrich)

  def findAllByCustomerIds(
      customerIds: Seq[UUID],
      loyaltyMembershipFilter: LoyaltyMembershipFilter = defaultFilter,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    implicit val merchant = user.toMerchantContext
    dao.findByCustomerIds(merchant.id, customerIds, loyaltyMembershipFilter).flatMap(enrich)
  }

  def findByCustomerIdAndLoyaltyProgramId(customerId: UUID, loyaltyProgramId: UUID)(implicit user: UserContext) =
    dao.findByCustomerIdAndLoyaltyProgramId(user.merchantId, customerId, loyaltyProgramId)

  def findOrCreateInActiveProgram(
      customerId: UUID,
      locationId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] = {
    implicit val merchant = user.toMerchantContext
    loyaltyProgramDao.findOneActiveLoyaltyProgram(user.merchantId, locationId).flatMap {
      case Some(loyaltyProgram) =>
        findByCustomerIdAndLoyaltyProgramId(customerId, loyaltyProgram.id).flatMap {
          case Some(status) => enrich(status).map(Some(_))
          case None         => create(customerId, loyaltyProgram.id)
        }
      case _ => Future.successful(None)
    }
  }

  private def create(customerId: UUID, loyaltyProgramId: UUID)(implicit user: UserContext): Future[Option[Entity]] = {
    implicit val merchant = user.toMerchantContext
    val update = toLoyaltyMembershipsUpdateWithLookupId(customerId = customerId, loyaltyProgramId = loyaltyProgramId)
    for {
      (_, record) <- dao.upsert(update)
      loyaltyMembership <- enrich(record)
      updatedLoyaltyMembership <- upsertPass(loyaltyMembership)
    } yield updatedLoyaltyMembership
  }

  def logOrderPoints(
      orderPointsData: OrderPointsData,
      loyaltyProgram: LoyaltyProgramRecord,
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Record]] =
    dao.findByRelIds(merchant.id, orderPointsData.customerId, loyaltyProgram.id).flatMap {
      _.fold[Future[Option[Record]]](Future.successful(None))(logOrderPoints(_, orderPointsData, loyaltyProgram))
    }

  private def logOrderPoints(
      loyaltyMembership: Record,
      orderPointsData: OrderPointsData,
      loyaltyProgram: LoyaltyProgramRecord,
      sendSqsMessage: Boolean = true,
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Record]] =
    for {
      updates <- loyaltyPointsHistoryService.convertToUpdates(loyaltyMembership, loyaltyProgram, orderPointsData)
      updatedLoyaltyMembership <- logPointsAndUpdateBalance(loyaltyMembership.id, updates, sendSqsMessage)
    } yield updatedLoyaltyMembership

  def logPointsAndUpdateBalance(
      loyaltyMembershipId: UUID,
      historyUpdates: Seq[LoyaltyPointsHistoryUpdate],
      sendSqsMessage: Boolean = true,
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Record]] =
    bulkLogPointsAndUpdateBalance(Map(loyaltyMembershipId -> historyUpdates), sendSqsMessage).map(_.headOption)

  def bulkLogPointsAndUpdateBalance(
      historyUpdatesPerLoyaltyMembership: Map[UUID, Seq[LoyaltyPointsHistoryUpdate]],
      sendSqsMessage: Boolean = true,
    )(implicit
      merchant: MerchantContext,
    ): Future[Seq[Record]] = {
    val loyaltyMembershipIds = historyUpdatesPerLoyaltyMembership.keys.toSeq
    for {
      _ <- dao.bulkLogPointsAndUpdateBalance(historyUpdatesPerLoyaltyMembership)
      records <- dao.findByIds(loyaltyMembershipIds)
      enrichedLoyaltyMemberships <- enrich(records)
      _ = if (sendSqsMessage) enrichedLoyaltyMemberships.foreach(messageHandler.sendLoyaltyMembershipChanged)
    } yield records
  }

  def upsertPass(loyaltyMembership: Entity)(implicit merchant: MerchantContext): Future[Option[Entity]] =
    loyaltyProgramService.findRecordById(loyaltyMembership.loyaltyProgramId).flatMap {
      case Some(lp) =>
        for {
          _ <- Future.sequence(PassType.values.map(upsertPass(_, loyaltyMembership, lp)))
          loyaltyMembership <- findById(loyaltyMembership.id)
        } yield loyaltyMembership
      case _ => Future.successful(Some(loyaltyMembership))
    }

  private def upsertPass(
      passType: PassType,
      loyaltyMembership: Entity,
      loyaltyProgramRecord: LoyaltyProgramRecord,
    )(implicit
      merchant: MerchantContext,
    ): Future[Unit] =
    loyaltyProgramRecord.templateIdByPassType(passType) match {
      case None =>
        logger.warn(s"Attempted to create pass $passType for passData $loyaltyMembership without a valid template id")
        Future.successful(None)
      case Some(templateId) =>
        urbanAirshipService.upsertPass(templateId, passType, loyaltyMembership).flatMap {
          case pass: Pass => updatePassPublicUrl(loyaltyMembership.id, passType, pass.publicUrl.path).void
          case _          => Future.successful(())
        }
    }

  private def updatePassPublicUrl(
      loyaltyMembershipId: UUID,
      passType: PassType,
      url: String,
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Entity]] =
    passType match {
      case PassType.Android => dao.updateAndroidPassPublicUrl(loyaltyMembershipId, url)
      case PassType.Ios     => dao.updateIosPassPublicUrl(loyaltyMembershipId, url)
    }

  def enrolViaMerchant(
      customerId: UUID,
      maybeLoyaltyProgram: Option[LoyaltyProgramRecord],
    )(implicit
      user: UserContext,
    ): Future[Option[Record]] =
    maybeLoyaltyProgram match {
      case Some(loyaltyProgram) =>
        implicit val m: MerchantContext = user.toMerchantContext
        val membershipFinder = dao.findByRelIds(user.merchantId, customerId, loyaltyProgram.id)
        detectOptInFieldChange(
          finder = membershipFinder,
          enroller = updateMerchantOptInAt(membershipFinder, customerId, loyaltyProgram.id),
        )
      case None => Future.successful(None)
    }

  private def updateMerchantOptInAt(
      membershipFinder: => Future[Option[Record]],
      customerId: UUID,
      loyaltyProgramId: UUID,
    )(implicit
      user: UserContext,
    ): Future[Option[Record]] =
    for {
      update <- convertToLoyaltyMembershipsUpdate(membershipFinder, customerId, loyaltyProgramId)
      merchantOptInUpdate = update.copy(merchantOptInAt = Some(UtcTime.now))
      (_, record) <- dao.upsertByRelIds(merchantOptInUpdate)
    } yield Some(record)

  private def convertToLoyaltyMembershipsUpdate(
      membershipFinder: => Future[Option[Record]],
      customerId: UUID,
      loyaltyProgramId: UUID,
    )(implicit
      user: UserContext,
    ): Future[LoyaltyMembershipUpdate] =
    dao.findByRelIds(user.merchantId, customerId, loyaltyProgramId).map {
      case Some(clp) => toLoyaltyMembershipsUpdate(clp)
      case _         => toLoyaltyMembershipsUpdateWithLookupId(customerId = customerId, loyaltyProgramId = loyaltyProgramId)
    }

  def enrolViaCustomer(loyaltyMembershipId: UUID, orderId: Option[UUID]): Future[Option[Record]] = {

    def customerEnroller(loyaltyMembershipId: UUID, orderId: Option[UUID])(implicit merchant: MerchantContext) =
      for {
        enrolledLoyaltyMembership <- OptionT(dao.updatedPassInstalledAtField(loyaltyMembershipId))
        reloadedLoyaltyMembership <- OptionT(logPointsFromReceiptOrderId(enrolledLoyaltyMembership, orderId))
      } yield reloadedLoyaltyMembership

    val optT = for {
      loyaltyMembership <- OptionT(dao.findById(loyaltyMembershipId))
      merchantContext <- OptionT(merchantService.getMerchantContext(loyaltyMembership.merchantId))
      result <- OptionT(
        detectOptInFieldChange(
          finder = dao.findById(loyaltyMembershipId),
          enroller = customerEnroller(loyaltyMembershipId, orderId)(merchantContext).value,
        )(merchantContext),
      )
    } yield result

    optT.value
  }

  private def logPointsFromReceiptOrderId(
      loyaltyMembership: Record,
      optOrderId: Option[UUID],
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Record]] =
    optOrderId.fold[Future[Option[Record]]](Future.successful(Some(loyaltyMembership))) { orderId =>
      val optT = for {
        orderPointsData <- OptionT(orderService.findOrderPointsDataById(orderId))
        loyaltyProgram <- OptionT(loyaltyProgramService.findRecordById(loyaltyMembership.loyaltyProgramId))
        updatedLoyaltyMembership <- OptionT(
          logOrderPoints(loyaltyMembership, orderPointsData, loyaltyProgram, sendSqsMessage = false),
        )
      } yield updatedLoyaltyMembership
      optT.value
    }

  private def detectOptInFieldChange(
      finder: => Future[Option[Record]],
      enroller: => Future[Option[Record]],
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Record]] =
    finder.flatMap {
      case Some(loyaltyMembership) if loyaltyMembership.isEnrolled => Future.successful(Some(loyaltyMembership))
      case _                                                       => enroller.flatMap(customerOptedIn)
    }

  private def customerOptedIn(maybeRecord: Option[Record])(implicit merchant: MerchantContext): Future[Option[Record]] =
    maybeRecord match {
      case Some(record) => customerOptedIn(record)
      case _            => Future.successful(maybeRecord)
    }

  private def customerOptedIn(record: Record)(implicit merchant: MerchantContext): Future[Option[Record]] =
    loyaltyProgramService.findById(record.loyaltyProgramId).flatMap {
      case Some(lp) =>
        val optT = for {
          updatedRecord <-
            if (lp.signupRewardEnabled.contains(true) && lp.signupRewardPoints.isDefined)
              OptionT(assignSignUpBonus(record, lp))
            else OptionT.some[Future](record)
          entity <- OptionT.liftF(enrich(updatedRecord))
          _ = messageHandler.prepareLoyaltyMembershipSignedUp(entity, lp)
        } yield updatedRecord
        optT.value
      case _ => Future.successful(Some(record))
    }

  def sendWelcomeEmail(loyaltyMembershipId: UUID)(implicit user: UserContext): Future[ErrorsOr[Unit]] = {
    implicit val m: MerchantContext = user.toMerchantContext

    val eitherT =
      for {
        loyaltyMembership <- EitherT(
          findById(loyaltyMembershipId)
            .map(
              Either
                .fromOption(_, NonAccessibleLoyaltyMembershipIds(Seq(loyaltyMembershipId)))
                .filterOrElse(_.enrolled, LoyaltyMembershipCustomerNotEnrolled(loyaltyMembershipId))
                .toEitherNel,
            ),
        )
        loyaltyProgram <- EitherT(
          loyaltyProgramService
            .findById(loyaltyMembership.loyaltyProgramId)
            .map(
              Either
                .fromOption(_, NonAccessibleLoyaltyProgramIds(Seq(loyaltyMembership.loyaltyProgramId)))
                .toEitherNel,
            ),
        )
        result <- EitherT(sendLoyaltyMembershipSignedUp(loyaltyMembership, loyaltyProgram).map(_.toEither))
      } yield result

    eitherT.value.map(_.toValidated)
  }

  def sendLoyaltyMembershipSignedUp(
      entity: Entity,
      loyaltyProgram: LoyaltyProgram,
    )(implicit
      merchant: MerchantContext,
    ): Future[ErrorsOr[Unit]] = {
    type Helper[+A] = Future[ValidatedNel[Error, A]]

    val futuresOfMerchantOrErrorAndCustomerMerchantOrError: (Helper[Merchant], Helper[CustomerMerchant]) = (
      merchantService
        .findById(merchant.id)(MerchantExpansions.none)
        .map(Either.fromOption(_, NonAccessibleMerchantIds(Seq(merchant.id))).toValidatedNel),
      customerMerchantService
        .findByCustomerId(entity.customerId)
        .map(Either.fromOption(_, NonAccessibleMerchantIds(Seq(entity.customerId))).toValidatedNel),
    )

    def send(
        barcodeUrl: String,
      )(
        merchantEntity: Merchant,
        customerEntity: CustomerMerchant,
      ): EitherNel[Error, Unit] =
      Either
        .fromOption(
          customerEntity.email.map(fireAndForget(merchantEntity, barcodeUrl)), {
            logger.error(s"Can't send loyalty sign up email for membership ${entity.id}")

            NonEmptyList.of(LoyaltyMembershipEmailNotSent(entity.id))
          },
        )

    def fireAndForget(merchantEntity: Merchant, barcodeUrl: String)(recipientEmail: String): Unit =
      messageHandler.sendLoyaltyMembershipSignedUp(recipientEmail, entity, merchantEntity, loyaltyProgram, barcodeUrl)

    val (a, b) = futuresOfMerchantOrErrorAndCustomerMerchantOrError

    (for {
      merchantOrError <- a
      customerOrError <- b
    } yield { barcodeUrl: String =>
      (merchantOrError, customerOrError)
        .mapN(send(barcodeUrl))
        .withEither(_.flatten)
    }).flatMap { sendWithBarcodeUrl =>
      barcodeService
        .generate(BarcodeMetadata.forLoyaltyMembershipSignedUp(entity))
        .map(sendWithBarcodeUrl)
    }

    // futuresOfMerchantOrErrorAndCustomerMerchantOrError
    //   .mapN { (merchantOrError, customerOrError) => barcodeUrl: String =>
    //     (merchantOrError, customerOrError)
    //       .mapN(send(barcodeUrl))
    //       .withEither(_.flatten)
    //   }
    //   .flatMap { sendWithBarcodeUrl =>
    //     barcodeService
    //       .generate(BarcodeMetadata.forLoyaltyMembershipSignedUp(entity))
    //       .map(sendWithBarcodeUrl)
    //   }
  }

  private def assignSignUpBonus(
      loyaltyMembership: Record,
      loyaltyProgram: LoyaltyProgram,
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Record]] = {
    val historyUpdates = Seq(toSignupBonusHistoryUpdate(loyaltyMembership, loyaltyProgram))
    logPointsAndUpdateBalance(loyaltyMembership.id, historyUpdates, sendSqsMessage = false)
  }

  implicit def toFutureOptionEntity(
      f: Future[Option[Record]],
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Entity]] = {
    val optionT = for {
      customerLoyaltyProgram <- OptionT(f)
      loyaltyMembership <- OptionT.liftF(enrich(customerLoyaltyProgram))
    } yield loyaltyMembership
    optionT.value
  }

  def findPerRewardRedemption(
      records: Seq[RewardRedemptionRecord],
    )(implicit
      merchant: MerchantContext,
    ): Future[Map[RewardRedemptionRecord, Entity]] =
    findByIds(records.map(_.loyaltyMembershipId)).map { loyaltyMemberships =>
      records.flatMap { record =>
        loyaltyMemberships
          .find(_.id == record.loyaltyMembershipId)
          .map(loyaltyMembership => record -> loyaltyMembership)
      }.toMap
    }

  def updateLinksWithOrderId(maybeLoyaltyMembership: Option[Entity], orderId: UUID): Option[Entity] =
    maybeLoyaltyMembership.map(updateLinksWithOrderId(_, orderId))

  private def updateLinksWithOrderId(loyaltyMembership: Entity, orderId: UUID): Entity = {
    val updatedPassUrls = generatePassUrls(loyaltyMembership, Some(orderId))
    loyaltyMembership.copy(passPublicUrls = updatedPassUrls)
  }

}
