package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.{ Product, TaxRate }
import io.paytouch.ordering.data.model.{ CartItemTaxRateRecord, CartItemTaxRateUpdate }
import io.paytouch.ordering.entities.{
  AppContext,
  CartItemUpsertion,
  MonetaryAmount,
  StoreContext,
  ValidCartItemUpsertion,
  CartItemTaxRate => CartItemTaxRateEntity,
}

trait CartItemTaxRateConversions {

  protected def fromRecordToEntity(record: CartItemTaxRateRecord)(implicit app: AppContext): CartItemTaxRateEntity =
    CartItemTaxRateEntity(
      id = record.id,
      taxRateId = record.taxRateId,
      name = record.name,
      `value` = record.`value`,
      total = MonetaryAmount(record.totalAmount),
      applyToPrice = record.applyToPrice,
    )

  protected def toItemTaxRateUpdateModels(
      itemId: UUID,
      upsertion: ValidCartItemUpsertion,
    )(implicit
      store: StoreContext,
    ): Option[Seq[CartItemTaxRateUpdate]] =
    upsertion.coreData.map { product =>
      val taxRates = product.locationOverrides.get(store.locationId).toSeq.flatMap(_.taxRates)
      val activeTaxRates = taxRates.filter(_.locationOverrides.get(store.locationId).exists(_.active))
      activeTaxRates.map(toItemTaxRateUpdateModel(itemId, _))
    }

  private def toItemTaxRateUpdateModel(
      itemId: UUID,
      taxRate: TaxRate,
    )(implicit
      store: StoreContext,
    ): CartItemTaxRateUpdate =
    CartItemTaxRateUpdate(
      id = None,
      storeId = Some(store.id),
      cartItemId = Some(itemId),
      taxRateId = Some(taxRate.id),
      name = Some(taxRate.name),
      value = Some(taxRate.value),
      totalAmount = Some(0),
      applyToPrice = Some(taxRate.applyToPrice),
    )
}
