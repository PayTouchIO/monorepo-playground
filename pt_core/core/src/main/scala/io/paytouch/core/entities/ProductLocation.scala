package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType

final case class ProductLocation(
    price: MonetaryAmount,
    cost: Option[MonetaryAmount],
    averageCost: Option[MonetaryAmount],
    unit: UnitType,
    margin: Option[BigDecimal],
    active: Option[Boolean],
    stock: Option[Stock],
    taxRates: Option[Seq[TaxRate]],
    taxRateIds: Option[Seq[UUID]],
    routeToKitchenId: Option[UUID],
  ) extends ItemLocationEntity

final case class ArticleLocationUpdate(
    price: BigDecimal,
    cost: Option[BigDecimal],
    unit: UnitType,
    margin: Option[BigDecimal],
    active: Option[Boolean],
    taxRateIds: Seq[UUID],
    routeToKitchenId: Option[UUID],
  ) extends ItemLocationUpdateEntity

trait ToArticleLocationUpdate[T] {

  def convert(t: T)(implicit user: UserContext): ArticleLocationUpdate

}

final case class ProductLocationUpdate(
    price: BigDecimal,
    cost: Option[BigDecimal],
    unit: UnitType,
    margin: Option[BigDecimal],
    active: Option[Boolean],
    taxRateIds: Seq[UUID],
    routeToKitchenId: Option[UUID],
  )

object ProductLocationUpdate extends ToArticleLocationUpdate[ProductLocationUpdate] {
  def convert(t: ProductLocationUpdate)(implicit user: UserContext) =
    ArticleLocationUpdate(
      price = t.price,
      cost = t.cost,
      unit = t.unit,
      margin = t.margin,
      active = t.active,
      taxRateIds = t.taxRateIds,
      routeToKitchenId = t.routeToKitchenId,
    )
}

final case class PartLocationUpdate(
    cost: Option[BigDecimal],
    unit: UnitType,
    active: Option[Boolean],
  )

object PartLocationUpdate extends ToArticleLocationUpdate[PartLocationUpdate] {
  def convert(t: PartLocationUpdate)(implicit user: UserContext) =
    ArticleLocationUpdate(
      price = 0,
      cost = t.cost,
      unit = t.unit,
      margin = None,
      active = t.active,
      taxRateIds = Seq.empty,
      routeToKitchenId = None,
    )
}

final case class RecipeLocationUpdate(
    cost: Option[BigDecimal],
    unit: UnitType,
    active: Option[Boolean],
  )

object RecipeLocationUpdate extends ToArticleLocationUpdate[RecipeLocationUpdate] {
  def convert(t: RecipeLocationUpdate)(implicit user: UserContext) =
    ArticleLocationUpdate(
      price = 0,
      cost = t.cost,
      unit = t.unit,
      margin = None,
      active = t.active,
      taxRateIds = Seq.empty,
      routeToKitchenId = None,
    )
}
