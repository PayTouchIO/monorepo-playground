package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object OrderItemModifierOptionSeeds extends Seeds {

  lazy val orderItemModifierOptionDao = daos.orderItemModifierOptionDao

  def load(
      orderItems: Seq[OrderItemRecord],
      modifierSetProducts: Seq[ModifierSetProductRecord],
      modifierSets: Seq[ModifierSetRecord],
      modifierOptions: Seq[ModifierOptionRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[OrderItemModifierOptionRecord]] = {

    val orderItemsWithAvailableModifiers = orderItems.filter { oi =>
      modifierSetProducts.exists(msp => oi.productId.contains(msp.productId))
    }

    val orderItemVariantOptions = orderItemsWithAvailableModifiers.random(ItemsWithModifierOption).map { orderItem =>
      val modifierSetProduct = modifierSetProducts.find(msp => orderItem.productId.contains(msp.productId))
      val modifierSet = modifierSets.find(ms => modifierSetProduct.map(_.modifierSetId).contains(ms.id))
      val modifierOption = modifierOptions.find(mo => modifierSet.map(_.id).contains(mo.modifierSetId))
      OrderItemModifierOptionUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        orderItemId = Some(orderItem.id),
        modifierOptionId = modifierOption.map(_.id),
        name = modifierOption.map(_.name),
        modifierSetName = modifierSet.map(_.name),
        `type` = modifierSet.map(_.`type`),
        priceAmount = modifierOption.map(_.priceAmount),
        quantity = Some(genBigDecimal.instance),
      )
    }

    orderItemModifierOptionDao.bulkUpsert(orderItemVariantOptions).extractRecords
  }
}
