package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.server.RequestContext

import cats.data._
import cats.implicits._

import io.paytouch._

import io.paytouch.core.BcryptRounds
import io.paytouch.core.conversions.MerchantConversions
import io.paytouch.core.data.daos.{ Daos, MerchantDao }
import io.paytouch.core.data.model.{
  LocationRecord,
  MerchantRecord,
  UserRecord,
  MerchantUpdate => MerchantUpdateModel,
  PaymentProcessorConfig,
}
import io.paytouch.core.data.model.enums.{ MerchantMode, PaymentProcessor }
import io.paytouch.core.entities.{ ApiMerchantUpdate => ApiMerchantUpdateEntity, _ }
import io.paytouch.core.entities.enums.{ MerchantSetupStatus, MerchantSetupSteps }
import io.paytouch.core.expansions.{ LoyaltyProgramExpansions, MerchantExpansions, NoExpansions }
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.features.{ FindByIdFeature, UpdateFeatureWithStateProcessing }
import io.paytouch.core.utils._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.MerchantValidator
import io.paytouch.utils.Tagging._
import io.paytouch.core.errors._
import io.paytouch.core.messages.SQSMessageHandler

class MerchantService(
    val adminMerchantService: AdminMerchantService,
    val authenticationService: AuthenticationService,
    val bcryptRounds: Int withTag BcryptRounds,
    val giftCardService: GiftCardService,
    val hmacService: HmacService,
    val locationService: LocationService,
    val locationReceiptService: LocationReceiptService,
    val loyaltyProgramService: LoyaltyProgramService,
    val messageHandler: SQSMessageHandler,
    sampleDataService: => SampleDataService,
    val userRoleService: UserRoleService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends MerchantConversions
       with FindByIdFeature
       with UpdateFeatureWithStateProcessing {
  type Dao = MerchantDao
  type Entity = Merchant
  type Filters = NoFilters
  type Expansions = MerchantExpansions
  type Model = MerchantUpdateModel
  type Record = MerchantRecord
  type State = Record
  type Update = ApiMerchantUpdateEntity
  type Upsertion = MerchantUpdateModel
  type Validator = MerchantValidator

  val defaultExpansions = MerchantExpansions.none
  val defaultFilters = NoFilters()

  protected val dao = daos.merchantDao
  protected val validator = new MerchantValidator

  def findById(merchantId: UUID)(e: Expansions): Future[Option[Entity]] =
    dao.findById(merchantId).flatMap(result => enrich(result.toSeq)(e).map(_.headOption))

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    enrich(records)(e)

  def enrich(records: Seq[Record])(e: Expansions): Future[Seq[Entity]] =
    for {
      setupStepsPerMerchant <- getOptionalSetupSteps(records)(e.withSetupSteps)
      firstLocationReceiptPerMerchant <- getFirstLocationReceipt(records)
      userOwnerPerMerchant <- getOptionalUserOwners(records)(e.withOwners)
      locationsPerMerchant <- getOptionalLocations(records)(e.withLocations)
      legalDetailsPerMerchant <- getOptionalLegalDetails(records)(e.withLegalDetails)
    } yield fromMerchantRecordsToEntities(
      records,
      firstLocationReceiptPerMerchant = firstLocationReceiptPerMerchant.get,
      setupStepsPerMerchant = setupStepsPerMerchant.get,
      userOwnerPerMerchant = userOwnerPerMerchant.get,
      locationsPerMerchant = locationsPerMerchant.get,
      legalDetailsPerMerchant = legalDetailsPerMerchant.get,
    )

  private def getOptionalSetupSteps(
      records: Seq[Record],
    )(
      withMerchantSetupSteps: Boolean,
    ): Future[Map[Record, Map[MerchantSetupSteps, MerchantSetupStatus]]] =
    Future.successful {
      if (withMerchantSetupSteps)
        records.map { record =>
          val allSteps =
            MerchantSetupSteps.forBusinessType(record.businessType).map(v => v -> MerchantSetupStatus.Pending).toMap
          val setupStatuses = record.setupSteps.map { ss =>
            val dbSteps = ss.transform {
              case (_, v) if v.completedAt.isDefined => MerchantSetupStatus.Completed
              case (_, v) if v.skippedAt.isDefined   => MerchantSetupStatus.Skipped
              case _                                 => MerchantSetupStatus.Pending
            }
            allSteps ++ dbSteps
          }
          record -> setupStatuses.getOrElse(allSteps)
        }.toMap
      else Map.empty
    }

  private def getFirstLocationReceipt(records: Seq[Record]): Future[Map[Record, LocationReceipt]] =
    Future
      .sequence {
        records.map { record =>
          locationReceiptService.findFirstByMerchantId(record.id)(MerchantContext.extract(record)).map {
            maybeLocationReceipt => maybeLocationReceipt.map(record -> _)
          }
        }
      }
      .map(_.flatten.toMap)

  private def getOptionalUserOwners(records: Seq[Record])(withOwners: Boolean): Future[Map[Record, UserRecord]] =
    if (withOwners)
      userService.findOwnerByMerchantIds(records.map(_.id)).map(_.mapKeysToRecords(records))
    else Future.successful(Map.empty)

  private def getOptionalLocations(
      records: Seq[Record],
    )(
      withLocations: Boolean,
    ): Future[Map[Record, Seq[LocationRecord]]] =
    if (withLocations)
      locationService.findByMerchantIds(records.map(_.id)).map(_.mapKeysToRecords(records))
    else
      Future.successful(Map.empty)

  private def getOptionalLegalDetails(
      records: Seq[Record],
    )(
      withLegalDetails: Boolean,
    ): Future[Map[Record, LegalDetails]] =
    Future.successful {
      if (withLegalDetails)
        records.flatMap(merchant => merchant.legalDetails.map(merchant -> _)).toMap
      else
        Map.empty
    }

  protected def convertToUpsertionModel(
      id: UUID,
      update: ApiMerchantUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    Multiple
      .success(fromApiMerchantUpdateEntityToUpdateModel(id, update))
      .pure[Future]

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(MerchantExpansions.none)
    } yield (resultType, enrichedRecord)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.successful(record)

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future.successful {
      if (update.businessName.isDefined && update.businessName != state.map(_.businessName)) {
        implicit val pagination = Pagination(1, 100)
        giftCardService.findAll(giftCardService.defaultFilters)(NoExpansions()).map {
          case (giftCards, _) =>
            giftCards.map { giftCard =>
              val giftCardUpdate = GiftCardUpdate.empty.copy(businessName = update.businessName)
              giftCardService.update(giftCard.id, giftCardUpdate)
            }
        }

        loyaltyProgramService.findAll(loyaltyProgramService.defaultFilters)(LoyaltyProgramExpansions.empty).map {
          case (loyaltyPrograms, _) =>
            loyaltyPrograms.map { loyaltyProgram =>
              val loyaltyProgramUpdate = LoyaltyProgramUpdate.empty.copy(businessName = update.businessName)
              loyaltyProgramService.update(loyaltyProgram.id, loyaltyProgramUpdate)
            }
        }
      }
    }

  def generatePath(merchantId: UUID): String = {
    val params = Map("merchant_id" -> merchantId.toString)
    hmacService.generateUri("/v1/merchants.create", params).toRelative.toString
  }

  def verifyUrl(requestContext: RequestContext): Boolean =
    hmacService.verifyUrl(requestContext)

  def skipSetupStep(
      step: MerchantSetupSteps,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessById(user.merchantId).flatMapTraverse { merchant =>
      updateStep(merchant, step, _.copy(skippedAt = Some(UtcTime.now)))
    }

  def completeSetupStep(
      step: MerchantSetupSteps,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessById(user.merchantId).flatMapTraverse { merchant =>
      updateStep(merchant, step, _.copy(completedAt = Some(UtcTime.now), skippedAt = None))
    }

  def completeSetupStepByMerchantId(
      merchantId: UUID,
      step: MerchantSetupSteps,
    ): Future[Unit] =
    dao.findById(merchantId).flatMap {
      case Some(merchant) => updateStep(merchant, step, _.copy(completedAt = Some(UtcTime.now), skippedAt = None))
      case None           => Future.unit
    }

  def resetSetupStep(
      step: MerchantSetupSteps,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessById(user.merchantId).flatMapTraverse { merchant =>
      updateStep(merchant, step, _.copy(completedAt = None, skippedAt = None))
    }

  def unvalidatedCompleteSetupStep(merchantId: UUID, step: MerchantSetupSteps): Future[Unit] =
    (for {
      merchant <- OptionT(dao.findById(merchantId))
      _ <- OptionT.liftF(updateStep(merchant, step, _.copy(completedAt = Some(UtcTime.now))))
    } yield ()).value.void

  private def updateStep(
      merchant: MerchantRecord,
      step: MerchantSetupSteps,
      f: MerchantSetupStep => MerchantSetupStep,
    ): Future[Unit] = {
    val currentSteps = merchant.setupSteps.getOrElse(Map.empty)
    val currentStep = currentSteps.getOrElse(step, MerchantSetupStep())
    val updatedStep = f(currentStep)
    val updatedSteps = currentSteps ++ Map(step -> updatedStep)

    val isDemoMode = merchant.mode == MerchantMode.Demo

    def stepsCompleted =
      MerchantSetupSteps.forBusinessType(merchant.businessType).forall { key =>
        updatedSteps.get(key).exists(value => value.completedAt.isDefined || value.skippedAt.isDefined)
      }

    val setupCompleted = isDemoMode || stepsCompleted

    dao
      .updateSetupSteps(merchant.id, setupCompleted, updatedSteps)
      .void
  }

  def getMerchantContext(merchantId: UUID): Future[Option[MerchantContext]] =
    dao.findMerchantContextById(merchantId)

  def prepareReceiptContext(
      recipientEmail: String,
      order: Order,
    )(implicit
      user: UserContext,
    ): Future[Option[ReceiptContext]] = {
    val optT = for {
      merchant <- OptionT(findById(user.merchantId)(MerchantExpansions.none))
      locationId <- OptionT.fromOption[Future](order.location.map(_.id))
      locationReceipt <- OptionT(locationReceiptService.findByLocationId(locationId))
    } yield ReceiptContext(
      recipientEmail,
      order,
      merchant,
      locationReceipt,
      loyaltyMembership = None,
      loyaltyProgram = None,
    )
    optT.value
  }

  def switchModeTo(switchMode: MerchantMode)(implicit user: UserContext): Future[ErrorsOr[Option[LoginResponse]]] =
    validator.validateSwitch(user.merchantId, switchMode).flatMapTraverse {
      case (merchant, owner) =>
        val optT = for {
          _ <- OptionT.liftF(authenticationService.deleteSessionsByMerchantId(merchant.id))
          switchMerchant <- OptionT(retrieveOrCreate(merchant, owner, switchMode))
          jwtToken <- OptionT(authenticationService.createSessionForOwner(switchMerchant.id))
        } yield jwtToken
        optT.value
    }

  private def retrieveOrCreate(
      merchant: MerchantRecord,
      owner: UserRecord,
      switchMode: MerchantMode,
    ): Future[Option[Entity]] =
    merchant.switchMerchantId match {
      case Some(switchMerchantId) => findById(switchMerchantId)(MerchantExpansions.none)
      case None                   => createSwitchMerchant(merchant, owner, switchMode)
    }

  private def createSwitchMerchant(
      merchant: MerchantRecord,
      owner: UserRecord,
      switchMode: MerchantMode,
    ): Future[Option[Entity]] = {
    val switchMerchantId = UUID.randomUUID
    val switchCreation = inferMerchantCreation(merchant, owner, switchMode)
    for {
      _ <- userService.renameEmailsByMerchantId(merchant.id, s"${merchant.mode.entryName}-")
      switchMerchantResult <- adminMerchantService.create(switchMerchantId, switchCreation)
      switchMerchant = switchMerchantResult.toOption.map { case (_, entity) => entity }
      _ <- userService.copyOwnerDataFromMerchant(merchant.id, switchMerchantId)
      _ <- dao.linkSwitchMerchants(merchant.id, switchMerchantId)
      _ <- sendMerchantChangedMessage(switchMerchantId)
    } yield switchMerchant
  }

  def sendMerchantChangedMessage(record: MerchantRecord): Future[Unit] =
    enrich(Seq(record))(MerchantExpansions.none).map {
      _.foreach { entity =>
        entity.mode match {
          case MerchantMode.Demo =>
          // We don't send messages to ordering regarding demo merchants

          case MerchantMode.Production =>
            import io.paytouch.implicits._
            messageHandler.sendMerchantChanged(entity, record.paymentProcessorConfig)
        }
      }
    }

  def sendMerchantChangedMessage(merchantId: UUID): Future[Unit] =
    dao.findById(merchantId).map(_.foreach(sendMerchantChangedMessage))

  def updatePaymentProcessorConfig(
      id: MerchantId,
      paymentProcessorConfig: PaymentProcessorConfig,
    ): Future[Unit] =
    dao
      .updatePaymentProcessorConfig(id.cast.get, paymentProcessorConfig.paymentProcessor, paymentProcessorConfig)
      .flatMap(_ => sendMerchantChangedMessage(id.cast.get.value))

  def getPaymentProcessorConfig[T <: PaymentProcessorConfig](
      processor: PaymentProcessor,
    )(implicit
      user: UserContext,
      m: Manifest[T],
    ): Future[ErrorsOr[T]] =
    validator
      .accessOneById(user.merchantId)
      .map {
        case Validated.Valid(merchant) =>
          merchant.paymentProcessorConfig match {
            case config: T =>
              Multiple.success(config)

            case _ =>
              Multiple.failure(MissingPaymentProcessorConfig(expected = processor))
          }

        case i @ Validated.Invalid(_) => i
      }

  def setWorldpayConfig(
      config: WorldpayConfigUpsertion,
    )(implicit
      context: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator
      .accessOneById(context.merchantId)
      .flatMap {
        case Validated.Valid(merchant) =>
          setWorldpayConfig(config.toPaymentProcessorConfig, merchant)

        case invalid =>
          invalid.void.pure[Future]
      }

  private def setWorldpayConfig(
      config: PaymentProcessorConfig.Worldpay,
      merchant: MerchantRecord,
    ): Future[ErrorsOr[Unit]] =
    merchant.paymentProcessor match {
      case PaymentProcessor.Paytouch | PaymentProcessor.Worldpay =>
        merchant.paymentProcessorConfig match {
          case _: PaymentProcessorConfig.Paytouch | _: PaymentProcessorConfig.Worldpay =>
            updatePaymentProcessorConfig(
              MerchantIdPostgres(merchant.id).cast,
              config,
            ).as(Multiple.success(()))

          case otherConfig =>
            Multiple
              .failure(
                UnexpectedPaymentProcessorConfig(
                  actual = otherConfig.paymentProcessor,
                  expected = PaymentProcessor.Paytouch,
                  PaymentProcessor.Worldpay,
                ),
              )
              .pure[Future]
        }

      case otherProcessor =>
        Multiple
          .failure(
            UnexpectedPaymentProcessor(
              actual = otherProcessor,
              expected = PaymentProcessor.Paytouch,
              PaymentProcessor.Worldpay,
            ),
          )
          .pure[Future]
    }

  def resetPaymentProcessor(
    )(implicit
      context: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator
      .accessOneById(context.merchantId)
      .flatMap {
        case Validated.Valid(merchant) =>
          updatePaymentProcessorConfig(
            MerchantIdPostgres(merchant.id).cast,
            PaymentProcessorConfig.Paytouch(),
          ).as(Multiple.success(()))

        case invalid =>
          invalid.void.pure[Future]
      }
}
