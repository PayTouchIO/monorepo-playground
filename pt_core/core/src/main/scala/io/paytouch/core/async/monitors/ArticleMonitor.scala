package io.paytouch.core.async.monitors

import java.util.UUID

import akka.actor.Actor
import io.paytouch.core.conversions.{ ProductCostHistoryConversions, ProductPriceHistoryConversions }
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ChangeReason, ImageUploadType }
import io.paytouch.core.entities.{
  ArticleLocationUpdate,
  UserContext,
  VariantArticleUpdate,
  ArticleUpdate => ArticleUpdateEntity,
}
import io.paytouch.core.services.{ ArticleService, ImageUploadService }

final case class ProductChange(
    state: ArticleService#State,
    update: ArticleUpdateEntity,
    userContext: UserContext,
  )

class ArticleMonitor(imageUploadService: ImageUploadService)(implicit val daos: Daos)
    extends Actor
       with ProductPriceHistoryConversions
       with ProductCostHistoryConversions {

  val productPriceHistoryDao = daos.productPriceHistoryDao
  val productCostHistoryDao = daos.productCostHistoryDao

  def receive: Receive = {
    case ProductChange(state, update, user) => recordProductChange(state, update, user)
  }

  def recordProductChange(
      state: ArticleService#State,
      update: ArticleUpdateEntity,
      user: UserContext,
    ) = {
    val (product, _, productLocations, images) = state

    recordImageChange(update, images)
    recordProductPriceChanges(product, productLocations, update)(user)
    recordProductCostChanges(product, productLocations, update)(user)
  }

  def recordImageChange(update: ArticleUpdateEntity, images: Seq[ImageUploadRecord]) =
    update.imageUploadIds.map { newImageIds =>
      val previousImageIds = images.map(_.id)
      val imageIdsToDelete = previousImageIds diff newImageIds
      imageUploadService.deleteImages(imageIdsToDelete, ImageUploadType.Product)
    }

  def recordProductPriceChanges(
      currentProduct: ArticleRecord,
      currentProductLocations: Seq[ProductLocationRecord],
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ) = {
    val productUpdates =
      detectProductPriceChanges(currentProductLocations.filter(_.productId == currentProduct.id), update)
    val variantUpdates = update
      .variantProducts
      .map {
        _.flatMap { variantUpdate =>
          val variantProductLocations = currentProductLocations.filter(_.productId == variantUpdate.id)
          detectProductVariantPriceChanges(variantProductLocations, variantUpdate, update.reason, update.notes)
        }
      }
      .getOrElse(Seq.empty)
    val updatesToConsider = (productUpdates ++ variantUpdates).filter(upd => upd.prevPriceAmount != upd.newPriceAmount)
    productPriceHistoryDao.bulkUpsert(updatesToConsider)
  }

  def detectProductPriceChanges(
      currentProductLocations: Seq[ProductLocationRecord],
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Seq[ProductPriceHistoryUpdate] = {
    val reason = update.reason
    val notes = update.notes
    val locationOverrides = update.locationOverrides
    detectPriceChanges(currentProductLocations, locationOverrides, reason, notes)
  }

  def detectProductVariantPriceChanges(
      currentProductLocations: Seq[ProductLocationRecord],
      update: VariantArticleUpdate,
      reason: ChangeReason,
      notes: Option[String],
    )(implicit
      user: UserContext,
    ): Seq[ProductPriceHistoryUpdate] = {
    val locationOverrides = update.locationOverrides
    detectPriceChanges(currentProductLocations, locationOverrides, reason, notes)
  }

  def detectPriceChanges(
      currentProductLocations: Seq[ProductLocationRecord],
      newLocationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
      reason: ChangeReason,
      notes: Option[String],
    )(implicit
      user: UserContext,
    ): Seq[ProductPriceHistoryUpdate] =
    newLocationOverrides.flatMap {
      case (locationId, Some(locationOverride)) =>
        currentProductLocations
          .filter(pl => pl.locationId == locationId && pl.priceAmount != locationOverride.price)
          .map(productLocation => toProductPriceHistoryUpdate(productLocation, locationOverride, reason, notes))
      case _ => Seq.empty
    }.toSeq

  def recordProductCostChanges(
      currentProduct: ArticleRecord,
      currentProductLocations: Seq[ProductLocationRecord],
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ) = {
    val productUpdates =
      detectProductCostChanges(currentProductLocations.filter(_.productId == currentProduct.id), update)
    val variantUpdates = update
      .variantProducts
      .map {
        _.flatMap { variantUpdate =>
          val variantProductLocations = currentProductLocations.filter(_.productId == variantUpdate.id)
          detectProductVariantCostChanges(variantProductLocations, variantUpdate, update.reason, update.notes)
        }
      }
      .getOrElse(Seq.empty)
    val updatedToConsider = (productUpdates ++ variantUpdates).filter(upd => upd.prevCostAmount != upd.newCostAmount)
    productCostHistoryDao.bulkUpsert(updatedToConsider)
  }

  def detectProductCostChanges(
      currentProductLocations: Seq[ProductLocationRecord],
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Seq[ProductCostHistoryUpdate] = {
    val locationOverrides = update.locationOverrides
    detectCostChanges(currentProductLocations, locationOverrides, update.reason, update.notes)
  }

  def detectProductVariantCostChanges(
      productLocations: Seq[ProductLocationRecord],
      update: VariantArticleUpdate,
      reason: ChangeReason,
      notes: Option[String],
    )(implicit
      user: UserContext,
    ): Seq[ProductCostHistoryUpdate] = {
    val locationOverrides = update.locationOverrides
    detectCostChanges(productLocations, locationOverrides, reason, notes)
  }

  def detectCostChanges(
      currentProductLocations: Seq[ProductLocationRecord],
      newLocationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
      reason: ChangeReason,
      notes: Option[String],
    )(implicit
      user: UserContext,
    ): Seq[ProductCostHistoryUpdate] =
    newLocationOverrides
      .filter { case (_, v) => v.flatMap(_.cost).isDefined }
      .flatMap {
        case (locationId, Some(locationOverride)) =>
          currentProductLocations
            .filter(pl =>
              pl.locationId == locationId && locationOverride.cost.isDefined && pl.costAmount != locationOverride.cost,
            )
            .map(productLocation => toProductCostHistoryUpdate(productLocation, locationOverride, reason, notes))
        case _ => Seq.empty
      }
      .toSeq

}
