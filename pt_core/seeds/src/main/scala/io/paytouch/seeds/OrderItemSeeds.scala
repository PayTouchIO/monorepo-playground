package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object OrderItemSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val orderItemDao = daos.orderItemDao

  def load(
      orders: Seq[OrderRecord],
      products: Seq[ArticleRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[OrderItemRecord]] = {

    val orderItems = orders.flatMap { order =>
      products.random(ItemsPerOrder).map { product =>
        val unit = genUnitType.instance
        val product = products.random

        val costAmount = genBigDecimal.instance
        val discountAmount = genBigDecimal.instance
        val taxAmount = genBigDecimal.instance
        val basePriceAmount = genBigDecimal.instance
        val totalPriceAmount = genBigDecimal.instance
        val profit = genBigDecimal.instance
        val priceAmount = profit + costAmount + discountAmount + taxAmount

        OrderItemUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          orderId = Some(order.id),
          productId = Some(product.id),
          productName = Some(product.name),
          productDescription = product.description,
          productType = Some(product.`type`),
          unit = Some(product.unit),
          quantity = Some(genQuantity(unit).instance),
          paymentStatus = Some(genPaymentStatus.instance),
          priceAmount = Some(priceAmount),
          costAmount = Some(costAmount),
          discountAmount = Some(discountAmount),
          taxAmount = Some(taxAmount),
          basePriceAmount = Some(basePriceAmount),
          calculatedPriceAmount = Some(priceAmount),
          totalPriceAmount = Some(totalPriceAmount),
          giftCardPassRecipientEmail = None,
          notes = Some(randomWords(5, allCapitalized = false)),
        )
      }
    }
    orderItemDao.bulkUpsert(orderItems).extractRecords
  }
}
