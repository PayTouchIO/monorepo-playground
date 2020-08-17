package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object OrderItemVariantOptionSeeds extends Seeds {

  lazy val orderItemVariantOptionDao = daos.orderItemVariantOptionDao

  def load(
      orderItems: Seq[OrderItemRecord],
      productVariantOptions: Seq[ProductVariantOptionRecord],
      variantOptions: Seq[VariantOptionRecord],
      variantOptionTypes: Seq[VariantOptionTypeRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[OrderItemVariantOptionRecord]] = {

    val orderItemsWithVariantProducts = orderItems.filter { oi =>
      productVariantOptions.exists(pvo => oi.productId.contains(pvo.productId))
    }

    val orderItemVariantOptions = orderItemsWithVariantProducts.flatMap { orderItem =>
      val productVariantOptionsPerProduct =
        productVariantOptions.filter(pvo => orderItem.productId.contains(pvo.productId))
      val variantOptionsPerProduct =
        variantOptions.filter(vo => productVariantOptionsPerProduct.map(_.variantOptionId).contains(vo.id))
      variantOptionsPerProduct.flatMap { variantOption =>
        variantOptionTypes.find(_.id == variantOption.variantOptionTypeId).map { variantOptionType =>
          OrderItemVariantOptionUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            orderItemId = Some(orderItem.id),
            variantOptionId = Some(variantOption.id),
            optionName = Some(variantOption.name),
            optionTypeName = Some(variantOptionType.name),
            position = Some(1),
          )
        }
      }
    }

    orderItemVariantOptionDao.bulkUpsertByRelIds(orderItemVariantOptions).extractRecords
  }
}
