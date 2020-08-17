package io.paytouch.core.async.monitors

import java.util.UUID

import akka.actor.Actor

import io.paytouch.implicits._

import io.paytouch.core.calculations.ProductStocksCalculations
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.QuantityChangeReason
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.UtcTime

final case class ProductQuantityIncrease(
    productLocation: ProductLocationRecord,
    prevQuantity: BigDecimal,
    newQuantity: BigDecimal,
    orderId: Option[UUID],
    reason: QuantityChangeReason,
    notes: Option[String],
    userContext: UserContext,
  )

class ProductQuantityHistoryMonitor(implicit daos: Daos) extends Actor with ProductStocksCalculations {
  val productQuantityHistoryDao =
    daos.productQuantityHistoryDao

  def receive: Receive = {
    case increase: ProductQuantityIncrease =>
      recordProductQuantityIncrease(increase)
  }

  def recordProductQuantityIncrease(increase: ProductQuantityIncrease) = {
    val updatesToConsider =
      Seq(toHistoryUpdate(increase)).filter(upd => upd.prevQuantityAmount != upd.newQuantityAmount)

    productQuantityHistoryDao.bulkUpsert(updatesToConsider)
  }

  private def toHistoryUpdate(increase: ProductQuantityIncrease): ProductQuantityHistoryUpdate = {
    val merchantId = increase.userContext.merchantId
    val productLocation = increase.productLocation
    val articleId = productLocation.productId
    val locationId = productLocation.locationId
    val userId = increase.userContext.id

    val stockValue =
      computeStockValue(
        productId = articleId,
        locationId = locationId,
        quantity = increase.newQuantity,
        productLocations = Seq(productLocation),
      )(increase.userContext)

    ProductQuantityHistoryUpdate(
      id = None,
      merchantId = Some(merchantId),
      productId = Some(articleId),
      locationId = Some(locationId),
      userId = Some(userId),
      orderId = increase.orderId,
      date = Some(UtcTime.now),
      prevQuantityAmount = Some(increase.prevQuantity),
      newQuantityAmount = Some(increase.newQuantity),
      newStockValueAmount = Some(stockValue.amount),
      reason = Some(increase.reason),
      notes = increase.notes,
    )
  }
}
