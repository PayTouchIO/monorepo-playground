package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.{ LocationOverridesPer, TaxRatesPerLocation }
import io.paytouch.core.data.model.{ TaxRateUpdate => TaxRateUpdateModel, _ }
import io.paytouch.core.entities.{
  ItemLocation,
  UserContext,
  TaxRate => TaxRateEntity,
  TaxRateUpdate => TaxRateUpdateEntity,
}

trait TaxRateConversions extends EntityConversion[TaxRateRecord, TaxRateEntity] {
  def groupTaxRatesPerProduct(
      productLocations: Seq[ProductLocationRecord],
      productLocationTaxRates: Seq[ProductLocationTaxRateRecord],
      taxRates: Seq[TaxRateEntity],
    ): Map[UUID, TaxRatesPerLocation] =
    productLocations.groupBy(_.productId).transform { (_, prodLocs) =>
      prodLocs.map { productLocation =>
        val taxRatesIds = productLocationTaxRates.filter(_.productLocationId == productLocation.id).map(_.taxRateId)
        productLocation.locationId -> taxRates.filter(tr => taxRatesIds.contains(tr.id))
      }.toMap
    }

  def groupTaxRatesPerLocation(
      taxRates: Seq[TaxRateEntity],
      taxRateLocations: Seq[TaxRateLocationRecord],
      locations: Seq[LocationRecord],
    ): Map[LocationRecord, Seq[TaxRateEntity]] =
    taxRateLocations.groupBy(_.locationId).mapKeysToRecords(locations).transform { (_, taxRateLocs) =>
      val taxRateIds = taxRateLocs.map(_.taxRateId)
      taxRates.filter(tR => taxRateIds.contains(tR.id))
    }

  def fromRecordToEntity(record: TaxRateRecord)(implicit user: UserContext) =
    fromRecordAndOptionsToEntity(record, None)

  def fromRecordsAndOptionsToEntities(
      taxRates: Seq[TaxRateRecord],
      locationOverridesPerProduct: Option[LocationOverridesPer[TaxRateRecord, ItemLocation]] = None,
    ) =
    taxRates.map { taxRate =>
      val locationOverrides = locationOverridesPerProduct.map(_.getOrElse(taxRate, Map.empty))
      fromRecordAndOptionsToEntity(taxRate, locationOverrides)
    }

  def fromRecordAndOptionsToEntity(taxRate: TaxRateRecord, locationOverrides: Option[Map[UUID, ItemLocation]]) =
    TaxRateEntity(
      id = taxRate.id,
      name = taxRate.name,
      value = taxRate.value,
      applyToPrice = taxRate.applyToPrice,
      locationOverrides = locationOverrides,
    )

  def fromUpsertionToUpdate(taxRateId: UUID, update: TaxRateUpdateEntity)(implicit user: UserContext) =
    TaxRateUpdateModel(
      id = Some(taxRateId),
      merchantId = Some(user.merchantId),
      name = update.name,
      value = update.value,
      applyToPrice = update.applyToPrice,
    )
}
