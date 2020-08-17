package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.GiftCardConversions
import io.paytouch.core.data.daos.{ Daos, GiftCardDao }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.GiftCardUpsertion
import io.paytouch.core.data.model.{ GiftCardRecord, GiftCardUpdate => GiftCardUpdateModel }
import io.paytouch.core.entities.enums.{ ExposedName, PassType }
import io.paytouch.core.entities.{
  GiftCardCreation,
  ImageUrls,
  UserContext,
  GiftCard => GiftCardEntity,
  GiftCardUpdate => GiftCardUpdateEntity,
}
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.GiftCardFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.GiftCardValidator
import io.paytouch.core.withTag

import scala.concurrent._

class GiftCardService(
    val articleService: ArticleService,
    val eventTracker: ActorRef withTag EventTracker,
    val imageUploadService: ImageUploadService,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GiftCardConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeatureWithStateProcessing
       with UpdateActiveItemFeature
       with DeleteFeature {

  type Creation = GiftCardCreation
  type Dao = GiftCardDao
  type Entity = GiftCardEntity
  type Expansions = NoExpansions
  type Filters = GiftCardFilters
  type Model = GiftCardUpsertion
  type Record = GiftCardRecord
  type Update = GiftCardUpdateEntity
  type Validator = GiftCardValidator
  type State = (Record)

  protected val dao = daos.giftCardDao
  protected val validator = new GiftCardValidator
  val defaultFilters = GiftCardFilters()

  val articleDao = daos.articleDao

  val classShortName = ExposedName.GiftCard

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val productIds = records.map(_.productId)
    val productsR = articleService.findByIds(productIds)
    val imageUrlsPerGiftCardsR = getImageUrlsPerGiftCard(records)
    for {
      products <- productsR
      imageUrlsPerGiftCards <- imageUrlsPerGiftCardsR
    } yield fromRecordsToEntities(records, products, imageUrlsPerGiftCards)
  }

  private def getImageUrlsPerGiftCard(records: Seq[Record]): Future[Map[Record, Seq[ImageUrls]]] = {
    val giftCardIds = records.map(_.id)
    imageUploadService.findByObjectIds(giftCardIds, ImageUploadType.GiftCard).map(_.mapKeysToRecords(records))
  }

  def toFutureResultTypeEntity(
      f: Future[(ResultType, GiftCardRecord)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, GiftCardEntity)] =
    for {
      (resultType, record) <- f
      entity <- enrich(record, defaultFilters)(NoExpansions())
    } yield (resultType, entity)

  protected def convertToUpsertionModel(
      id: UUID,
      update: GiftCardUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[GiftCardUpsertion]] =
    for {
      validGiftUpdate <- convertToGiftCardUpdate(id, update)
      validProductUpdate <- articleService.convertToUpsertionModel(update, validGiftUpdate.toOption)
      validImageUploads <-
        imageUploadService
          .convertToImageUploadUpdates(id, ImageUploadType.GiftCard, update.imageUploadIds)
    } yield Multiple.combine(validGiftUpdate, validProductUpdate, validImageUploads)(GiftCardUpsertion)

  private def convertToGiftCardUpdate(
      id: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[GiftCardUpdateModel]] =
    validator.validateUpsertion(id).mapNested { record =>
      val productId = record.map(_.productId).getOrElse(UUID.randomUUID)
      fromUpsertionToUpdate(id, productId, upsertion)
    }

  override def bulkDelete(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    for {
      records <- dao.findByIdsAndMerchantId(ids, user.merchantId)
      productIds = records.map(_.productId)
      _ <- articleService.bulkDelete(productIds)
      result <- super.bulkDelete(ids)
    } yield result

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
    Future.successful(messageHandler.sendGiftCardChangedMsg(entity))

  def findRecordById(id: UUID) = dao.findById(id)

  def updateTemplateId(
      id: UUID,
      passType: PassType,
      templateId: String,
    ) = {
    val update = passType match {
      case PassType.Ios     => GiftCardUpdateModel.empty.copy(id = Some(id), appleWalletTemplateId = Some(templateId))
      case PassType.Android => GiftCardUpdateModel.empty.copy(id = Some(id), androidPayTemplateId = Some(templateId))
    }
    dao.upsert(update)
  }
}
