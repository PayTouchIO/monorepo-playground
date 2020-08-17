package io.paytouch.core.conversions

import io.paytouch.core.data.model.OrderBundleRecord
import io.paytouch.core.entities.{ OrderBundle, OrderBundleOption, OrderBundleSet }
import io.paytouch.core.validators.{ RecoveredOrderBundleOption, RecoveredOrderBundleSet }

trait OrderBundleConversions {

  protected def fromRecordToEntity(record: OrderBundleRecord): OrderBundle =
    OrderBundle(
      id = record.id,
      bundleOrderItemId = record.bundleOrderItemId,
      orderBundleSets = record.orderBundleSets.map(toOrderBundleSet),
    )

  protected def toOrderBundleSet(orderBundleSet: RecoveredOrderBundleSet): OrderBundleSet =
    OrderBundleSet(
      id = orderBundleSet.id,
      name = orderBundleSet.name,
      bundleSetId = orderBundleSet.bundleSetId,
      position = orderBundleSet.position,
      orderBundleOptions = orderBundleSet.orderBundleOptions.map(toOrderBundleOption),
    )

  protected def toOrderBundleOption(orderBundleOption: RecoveredOrderBundleOption): OrderBundleOption =
    OrderBundleOption(
      id = orderBundleOption.id,
      bundleOptionId = orderBundleOption.bundleOptionId,
      articleOrderItemId = orderBundleOption.articleOrderItemId,
      position = orderBundleOption.position,
      priceAdjustment = orderBundleOption.priceAdjustment,
    )
}
