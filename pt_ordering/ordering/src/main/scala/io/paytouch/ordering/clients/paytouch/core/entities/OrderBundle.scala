package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class OrderBundle(
    id: UUID,
    bundleOrderItemId: UUID,
    orderBundleSets: Seq[OrderBundleSet],
  )

final case class OrderBundleSet(
    id: UUID,
    bundleSetId: Option[UUID],
    name: Option[String],
    position: Int,
    orderBundleOptions: Seq[OrderBundleOption],
  )

final case class OrderBundleOption(
    id: UUID,
    bundleOptionId: Option[UUID],
    articleOrderItemId: Option[UUID],
    position: Int,
    priceAdjustment: BigDecimal,
  )

final case class OrderBundleUpsertion(
    id: UUID,
    bundleOrderItemId: UUID,
    orderBundleSets: Seq[OrderBundleSetUpsertion],
  )

final case class OrderBundleSetUpsertion(
    id: UUID,
    bundleSetId: UUID,
    name: Option[String],
    position: Int,
    orderBundleOptions: Seq[OrderBundleOptionUpsertion],
  )

final case class OrderBundleOptionUpsertion(
    id: UUID,
    bundleOptionId: UUID,
    articleOrderItemId: UUID,
    position: Int,
    priceAdjustment: BigDecimal,
  )
