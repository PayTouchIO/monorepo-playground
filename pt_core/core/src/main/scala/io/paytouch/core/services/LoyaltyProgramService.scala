package io.paytouch.core.services

import java.util.UUID

import cats.data.OptionT
import cats.implicits._
import io.paytouch.core.conversions.LoyaltyProgramConversions
import io.paytouch.core.data.daos.{ Daos, LoyaltyProgramDao }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LoyaltyProgramUpsertion
import io.paytouch.core.data.model.{
  LoyaltyProgramRecord,
  OrderRecord,
  LoyaltyProgramUpdate => LoyaltyProgramUpdateModel,
}
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.{
  ImageUrls,
  Location,
  LoyaltyProgramCreation,
  LoyaltyReward,
  MerchantContext,
  OrderPointsData,
  UserContext,
  LoyaltyProgram => LoyaltyProgramEntity,
  LoyaltyProgramUpdate => LoyaltyProgramUpdateEntity,
}
import io.paytouch.core.expansions.LoyaltyProgramExpansions
import io.paytouch.core.filters.LoyaltyProgramFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.LoyaltyProgramValidator
import io.paytouch.core.RichMap
import io.paytouch.core.utils._

import scala.concurrent._

class LoyaltyProgramService(
    loyaltyMembershipService: => LoyaltyMembershipService,
    val imageUploadService: ImageUploadService,
    val locationService: LocationService,
    val loyaltyProgramLocationService: LoyaltyProgramLocationService,
    loyaltyRewardService: => LoyaltyRewardService,
    val messageHandler: SQSMessageHandler,
    val paymentTransactionService: PaymentTransactionService,
    val orderItemService: OrderItemService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LoyaltyProgramConversions
       with CreateAndUpdateFeatureWithStateProcessing
       with FindAllFeature
       with FindByIdFeature {

  type Creation = LoyaltyProgramCreation
  type Dao = LoyaltyProgramDao
  type Entity = LoyaltyProgramEntity
  type Expansions = LoyaltyProgramExpansions
  type Filters = LoyaltyProgramFilters
  type Model = LoyaltyProgramUpsertion
  type Record = LoyaltyProgramRecord
  type Update = LoyaltyProgramUpdateEntity
  type Validator = LoyaltyProgramValidator

  type State = Record

  protected val dao = daos.loyaltyProgramDao
  val defaultFilters = LoyaltyProgramFilters()

  protected val validator = new LoyaltyProgramValidator

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext) = {
    implicit val merchant = user.toMerchantContext
    val locationsPerLoyaltyProgramR =
      getOptionalLocations(records)(e.withLocations)
    val rewardsPerLoyaltyProgramR = getRewardsPerLoyaltyProgram(records)
    val imageUrlsPerLoyaltyProgramsR = getImageUrlsPerLoyaltyProgram(records)
    for {
      locationsPerLoyaltyProgram <- locationsPerLoyaltyProgramR
      rewardsPerLoyaltyProgram <- rewardsPerLoyaltyProgramR
      imageUrlsPerLoyaltyPrograms <- imageUrlsPerLoyaltyProgramsR
    } yield fromRecordsAndOptionsToEntities(
      records,
      rewardsPerLoyaltyProgram,
      locationsPerLoyaltyProgram,
      imageUrlsPerLoyaltyPrograms,
    )
  }

  private def getOptionalLocations(
      items: Seq[Record],
    )(
      withLocations: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[Location]] =
    if (withLocations) {
      val loyaltyProgramIds = items.map(_.id)
      locationService.findAllByLoyaltyProgramIds(loyaltyProgramIds).map { locationsPerLoyaltyProgramId =>
        val locationsPerLoyaltyProgram = locationsPerLoyaltyProgramId.mapKeysToRecords(items)
        Some(locationsPerLoyaltyProgram)
      }
    }
    else Future.successful(None)

  private def getRewardsPerLoyaltyProgram(
      items: Seq[Record],
    )(implicit
      user: UserContext,
    ): Future[Map[Record, Seq[LoyaltyReward]]] = {
    val itemIds = items.map(_.id)
    loyaltyRewardService.findByLoyaltyProgramIds(itemIds).map(_.mapKeysToRecords(items))
  }

  private def getImageUrlsPerLoyaltyProgram(records: Seq[Record]): Future[Map[Record, Seq[ImageUrls]]] = {
    val loyaltyProgramIds = records.map(_.id)
    imageUploadService
      .findByObjectIds(loyaltyProgramIds, ImageUploadType.LoyaltyProgram)
      .map(_.mapKeysToRecords(records))
  }

  def findAllByCustomerIds(customerIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] = {
    implicit val merchant = user.toMerchantContext
    for {
      loyaltyMemberships <- loyaltyMembershipService.findAllByCustomerIds(customerIds)
      loyaltyPrograms <- findByIds(loyaltyMemberships.map(_.loyaltyProgramId))
    } yield groupLoyaltyProgramsPerCustomer(loyaltyMemberships, loyaltyPrograms)
  }

  def findById(loyaltyProgramId: UUID)(implicit merchant: MerchantContext): Future[Option[Entity]] =
    findByIds(Seq(loyaltyProgramId)).map(_.headOption)

  def findByIds(loyaltyProgramIds: Seq[UUID])(implicit merchant: MerchantContext): Future[Seq[Entity]] =
    dao.findByIds(loyaltyProgramIds).map(fromRecordsToEntities)

  def findByOptId(loyaltyProgramId: Option[UUID])(implicit merchant: MerchantContext): Future[Option[Entity]] =
    findByIds(loyaltyProgramId.toSeq).map(_.headOption)

  override def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      entity <- enrich(record, defaultFilters)(LoyaltyProgramExpansions(withLocations = false))
    } yield (resultType, entity)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      loyaltyProgram <- convertToLoyaltyProgramUpdate(id, update)
      loyaltyProgramLocations <-
        loyaltyProgramLocationService
          .convertToLoyaltyProgramLocationUpdates(id, update.locationIds)
      loyaltyRewards <- loyaltyRewardService.convertToLoyaltyRewardUpdates(id, update.rewards)
      imageUploads <-
        imageUploadService
          .convertToImageUploadUpdates(id, ImageUploadType.LoyaltyProgram, update.imageUploadIds)
    } yield Multiple.combine(loyaltyProgram, loyaltyProgramLocations, loyaltyRewards, imageUploads)(
      LoyaltyProgramUpsertion,
    )

  private def convertToLoyaltyProgramUpdate(
      loyaltyProgramId: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[LoyaltyProgramUpdateModel]] =
    validator.validateNoMoreThanOne(loyaltyProgramId).mapNested { _ =>
      fromUpsertionToUpdate(loyaltyProgramId, upsertion)
    }

  def logPoints(record: OrderRecord)(implicit merchant: MerchantContext): Future[Unit] = {
    val optT = for {
      loyaltyProgram <- OptionT(dao.findOneActiveLoyaltyProgram(merchant.id, record.locationId))
      paymentTransactions <- OptionT.liftF(paymentTransactionService.findByOrderIds(Seq(record.id)))
      orderItems <- OptionT.liftF(orderItemService.findRecordsByOrderIds(Seq(record.id)))
      orderPointsData <- OptionT.fromOption[Future](OrderPointsData.extract(record, paymentTransactions, orderItems))
      result <- OptionT(loyaltyMembershipService.logOrderPoints(orderPointsData, loyaltyProgram))
    } yield result
    optT.value.void
  }

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

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
    Future.successful(messageHandler.sendLoyaltyProgramChangedMsg(entity))

  def findRecordById(id: UUID) = dao.findById(id)

  def updateTemplateId(
      id: UUID,
      passType: PassType,
      templateId: String,
    ) = {
    val update = passType match {
      case PassType.Ios =>
        LoyaltyProgramUpdateModel.empty.copy(id = Some(id), appleWalletTemplateId = Some(templateId))
      case PassType.Android =>
        LoyaltyProgramUpdateModel.empty.copy(id = Some(id), androidPayTemplateId = Some(templateId))
    }
    dao.upsert(update)
  }
}
