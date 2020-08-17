package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.{ TaxRateIdsPerLocation, TaxRatesPerLocation }
import io.paytouch.core.data.model.{ ProductLocationRecord, ProductLocationUpdate => ProductLocationUpdateModel }
import io.paytouch.core.entities._

trait ProductLocationConversions {

  def toProductLocationUpdate(
      locationOverride: ArticleLocationUpdate,
      productId: UUID,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): ProductLocationUpdateModel =
    ProductLocationUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      locationId = Some(locationId),
      priceAmount = Some(locationOverride.price),
      costAmount = locationOverride.cost,
      averageCostAmount = None,
      unit = Some(locationOverride.unit),
      margin = locationOverride.margin,
      active = locationOverride.active,
      routeToKitchenId = locationOverride.routeToKitchenId,
    )

  def toProductLocationUpdateMap(
      productId: UUID,
      locationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
      existingProductLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserContext,
    ): Map[UUID, Option[ProductLocationUpdateModel]] =
    locationOverrides.map {
      case (locationId, locationOverride) =>
        locationId -> locationOverride.map { locPrice =>
          val existingId = existingProductLocations.find(_.contains(productId, locationId)).map(_.id)
          toProductLocationUpdate(locPrice, productId, locationId).copy(id = existingId.orElse(Some(UUID.randomUUID)))
        }
    }

  def fromItemLocationsToProductLocations(
      itemLocations: Seq[ProductLocationRecord],
      stockPerLocation: Map[UUID, Stock],
      taxRatesPerLocation: TaxRatesPerLocation,
      taxRateIdsPerLocation: TaxRateIdsPerLocation,
    )(implicit
      user: UserContext,
    ) =
    itemLocations.map(itemLoc =>
      itemLoc.locationId -> fromItemLocationToProductLocation(
        itemLoc,
        stockPerLocation.get(itemLoc.locationId),
        taxRatesPerLocation.get(itemLoc.locationId),
        taxRateIdsPerLocation.get(itemLoc.locationId),
      ),
    )

  def fromItemLocationToProductLocation(
      record: ProductLocationRecord,
      stock: Option[Stock],
      taxRates: Option[Seq[TaxRate]],
      taxRateIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): ProductLocation =
    ProductLocation(
      price = MonetaryAmount(record.priceAmount),
      cost = MonetaryAmount.extract(record.costAmount),
      averageCost = MonetaryAmount.extract(record.averageCostAmount),
      unit = record.unit,
      margin = record.margin,
      active = Some(record.active),
      stock = stock,
      routeToKitchenId = record.routeToKitchenId,
      taxRates = taxRates,
      taxRateIds = taxRateIds,
    )
}
