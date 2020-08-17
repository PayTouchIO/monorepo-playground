package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.conversions.StoreConversions
import io.paytouch.ordering.data.daos.{ Daos, StoreDao }
import io.paytouch.ordering.data.model
import io.paytouch.ordering.entities
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.messages.SQSMessageHandler
import io.paytouch.ordering.services.features._
import io.paytouch.ordering.validators.StoreValidator

class StoreService(
    imageService: ImageService,
    merchantService: MerchantService,
    messageHandler: SQSMessageHandler,
    ptCoreClient: PtCoreClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends StoreConversions
       with FindByIdFeature
       with FindAllFeature
       with CreateFeature
       with StandardUpdateFeature
       with UpdateActiveItemFeature {
  type Context = entities.UserContext
  type Creation = entities.StoreCreation
  type Dao = StoreDao
  type Entity = entities.Store
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = model.StoreUpdate
  type Record = model.StoreRecord
  type Validator = StoreValidator
  type Upsertion = entities.StoreUpdate

  val dao = daos.storeDao

  protected val validator = new StoreValidator(ptCoreClient)

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = {
    val merchantExpander = MandatoryExpander[this.type, (String, PaymentProcessor)](
      retrieverF = { implicit context => stores =>
        merchantService
          .findByStores(stores)
          .map(m => m.transform((_, merchant) => merchant.urlSlug -> merchant.paymentProcessor))
      },
      copierF = {
        case (entity, (slug, paymentProcessor)) =>
          entity.copy(
            merchantUrlSlug = slug,
            paymentMethods = MergeValidPaymentMethodsWithActiveOnes(entity.paymentMethods, paymentProcessor),
          )
      },
      defaultDataValue = "" -> PaymentProcessor.Paytouch,
    )

    Seq(merchantExpander)
  }

  protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      user: entities.UserContext,
    ): Future[Model] =
    Future.successful(toUpsertionModel(id, upsertion))

  def findBySlugs(merchantSlug: String, slug: String): Future[Option[Entity]] =
    (for {
      record <- OptionT(dao.findByMerchantSlugAndSlug(merchantSlug = merchantSlug, slug = slug))
      context = entities.StoreContext.fromRecord(record)
      entity <- OptionT.liftF(enrich(record)(context))
    } yield entity).value

  def findAllSimpleByMerchantId(merchantId: UUID): Future[Seq[entities.SimpleStore]] =
    dao.findByMerchantId(merchantId).map(fromRecordsToSimpleStores)

  override protected def processingAfterUpdateActive(
      storeItems: Seq[entities.UpdateActiveItem],
      records: Seq[model.StoreRecord],
    )(implicit
      user: entities.UserContext,
    ): Future[Unit] =
    Future.successful {
      val storeToLocation: UUID => Option[UUID] =
        records
          .map(record => record.id -> record.locationId)
          .toMap
          .get

      val locationItems = storeItems.flatMap { storeItem =>
        storeToLocation(storeItem.itemId).map(locationId => storeItem.copy(itemId = locationId))
      }
    }

  override def processAfterUpsert(
      currentEntity: entities.Store,
      previousRecord: Option[model.StoreRecord],
    )(implicit
      user: entities.UserContext,
    ): Future[Unit] = {
    val id = currentEntity.locationId // core doesn't know what to make of a store_id

    val imageHandlers = List(
      imageService.notifyNewImageIds(id, currentEntity, previousRecord)(_.heroImageUrls, _.heroImageUrls),
      imageService.notifyNewImageIds(id, currentEntity, previousRecord)(_.logoImageUrls, _.logoImageUrls),
      imageService.notifyDeletedImageIds(currentEntity, previousRecord)(_.heroImageUrls, _.heroImageUrls),
      imageService.notifyDeletedImageIds(currentEntity, previousRecord)(_.logoImageUrls, _.logoImageUrls),
    )

    val storeCreatedHandler = previousRecord match {
      case Some(_) => List.empty
      case None =>
        List(
          messageHandler
            .sendStoreCreated(currentEntity.locationId)
            .pure[Future],
        )
    }

    (storeCreatedHandler ++ imageHandlers).sequence.void
  }

  def enableProcessorPaymentMethodForAllStores(merchant: model.MerchantRecord): Future[Unit] =
    dao.findByMerchantId(merchant.id).flatMap { stores =>
      stores
        .map { store =>
          val updatedPaymentMethods: Seq[entities.PaymentMethod] = {
            val paymentMethods = MergeValidPaymentMethodsWithActiveOnes(store.paymentMethods, merchant.paymentProcessor)
            val targetPaymentMethodType = MergeValidPaymentMethodsWithActiveOnes.toMethodType(merchant.paymentProcessor)

            (paymentMethods
              .map(pm => pm.`type` -> pm.active)
              .toMap + (targetPaymentMethodType -> true)).map(entities.PaymentMethod.tupled).toSeq
          }

          store
            .copy(paymentMethods = updatedPaymentMethods)
            .deriveUpdateFromPreviousState(store)
        }
        .pipe(dao.bulkUpsert)
        .void
    }
}
