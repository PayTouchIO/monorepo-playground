package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ProductLocationUpdate => ProductLocationUpdateModel, _ }
import io.paytouch.core.entities.{ UserContext, ArticleLocationUpdate => ProductLocationUpdateEntity }

trait TaxRateLocationConversions {

  def toProductLocationTaxRateUpdateMap(
      productId: UUID,
      locationOverride: Map[UUID, Option[ProductLocationUpdateEntity]],
      productLocations: Seq[ProductLocationUpdateModel],
    )(implicit
      user: UserContext,
    ): Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]] =
    locationOverride.map {
      case (locationId, productLocationUpdate) =>
        locationId -> productLocationUpdate.flatMap { prodLocUp =>
          val productLocation =
            productLocations.find(pl => pl.productId.contains(productId) && pl.locationId.contains(locationId))
          productLocation.flatMap(toProductLocationTaxRateUpdates(prodLocUp, _))
        }
    }

  def toProductLocationTaxRateUpdates(
      productLocationUpdateEntity: ProductLocationUpdateEntity,
      productLocation: ProductLocationUpdateModel,
    )(implicit
      user: UserContext,
    ): Option[Seq[ProductLocationTaxRateUpdate]] =
    productLocation.id.map(toProductLocationTaxRateUpdates(_, productLocationUpdateEntity.taxRateIds))

  def toProductLocationTaxRateUpdates(
      productLocationId: UUID,
      taxRateIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[ProductLocationTaxRateUpdate] =
    taxRateIds.map(toProductLocationTaxRateUpdate(productLocationId, _))

  def toProductLocationTaxRateUpdate(
      productLocationId: UUID,
      taxRateId: UUID,
    )(implicit
      user: UserContext,
    ): ProductLocationTaxRateUpdate =
    ProductLocationTaxRateUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productLocationId = Some(productLocationId),
      taxRateId = Some(taxRateId),
    )

  def toTaxRateLocationUpdate(taxRateId: UUID, locationId: UUID)(implicit user: UserContext): TaxRateLocationUpdate =
    TaxRateLocationUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(user.merchantId),
      taxRateId = Some(taxRateId),
      locationId = Some(locationId),
      active = None,
    )

  def toTaxRateLocationUpdate(record: TaxRateLocationRecord)(implicit user: UserContext): TaxRateLocationUpdate =
    TaxRateLocationUpdate(
      id = Some(record.id),
      merchantId = Some(user.merchantId),
      taxRateId = Some(record.taxRateId),
      locationId = Some(record.locationId),
      active = Some(record.active),
    )
}
