package io.paytouch.core.async.importers.extractors

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.async.importers.parsers.{ EnrichedDataRow, ValidationError }
import io.paytouch.core.async.importers.{ Keys, Rows }
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait ProductLocationExtractor extends Extractor {

  private val productLocationDao = daos.productLocationDao
  private val kitchenDao = daos.kitchenDao

  def extractProductLocations(
      data: Rows,
      products: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ErrorsOr[Seq[ProductLocationUpdate]]] = {
    logExtraction("product locations")
    val productIds = products.flatMap(_.id)
    val locationIds = locations.map(_.id)
    val merchantId = importRecord.merchantId
    for {
      existingProductLocations <- productLocationDao.findByItemIdsAndLocationIds(productIds, locationIds)
      kitchens <- kitchenDao.findByMerchantAndLocationIds(merchantId, locationIds)
    } yield buildProductLocationUpdates(data, products, locations, existingProductLocations, kitchens)
  }

  private def buildProductLocationUpdates(
      data: Rows,
      products: Seq[ArticleUpdate],
      locations: Seq[LocationRecord],
      existingProductLocations: Seq[ProductLocationRecord],
      kitchens: Seq[KitchenRecord],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[ProductLocationUpdate]] = {
    val extractions = for {
      product <- products
      location <- locations
    } yield {
      val existingId = existingProductLocations
        .find(pl => pl.locationId == location.id && product.id.contains(pl.productId))
        .map(_.id)
      val id = existingId.getOrElse(UUID.randomUUID)
      val row = data.find(_.isForProductId(product.id))

      extractKitchenId(row, location, kitchens) match {
        case Valid(maybeKitchenId) =>
          MultipleExtraction.success(Seq(toProductLocationUpdate(id, product, location, maybeKitchenId)))
        case i @ Invalid(_) => i
      }
    }
    MultipleExtraction.sequence(extractions)
  }

  private def extractKitchenId(
      maybeRow: Option[EnrichedDataRow],
      location: LocationRecord,
      kitchens: Seq[KitchenRecord],
    ): ErrorsOr[Option[UUID]] =
    maybeRow match {
      case Some(row) =>
        val name = row.data.get(Keys.Kitchen).flatMap(_.headOption)
        if (name.isEmpty)
          MultipleExtraction.success(None)
        else
          kitchens
            .find(k => k.locationId == location.id && name.contains(k.name)) match {
            case Some(kitchen) =>
              MultipleExtraction.success(Some(kitchen.id))

            case _ =>
              MultipleExtraction.failure(
                ValidationError(
                  Some(row.rowNumber),
                  s"Invalid kitchen name ${name.get}",
                ),
              )
          }

      case _ =>
        MultipleExtraction.success(None)
    }

  private def toProductLocationUpdate(
      id: UUID,
      product: ArticleUpdate,
      location: LocationRecord,
      kitchenId: Option[UUID],
    )(implicit
      importRecord: ImportRecord,
    ): ProductLocationUpdate =
    ProductLocationUpdate(
      id = Some(id),
      merchantId = Some(importRecord.merchantId),
      productId = product.id,
      locationId = Some(location.id),
      priceAmount = product.priceAmount,
      costAmount = product.costAmount,
      averageCostAmount = None,
      unit = product.unit,
      margin = product.margin,
      active = product.active,
      routeToKitchenId = kitchenId,
    )
}
