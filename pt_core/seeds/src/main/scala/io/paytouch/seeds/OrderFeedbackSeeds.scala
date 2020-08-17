package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.IdsProvider._
import org.scalacheck.Gen

import scala.concurrent._

object OrderFeedbackSeeds extends Seeds {

  lazy val orderFeedbackDao = daos.orderFeedbackDao

  def load(orders: Seq[OrderRecord])(implicit user: UserRecord): Future[Seq[OrderFeedbackRecord]] = {
    val orderFeedbackIds = orderFeedbackIdsPerEmail(user.email)

    val ordersWithCustomer = orders.filter(_.customerId.isDefined)

    val orderFeedbacks = orderFeedbackIds.zip(ordersWithCustomer.shuffle).map {
      case (orderFeedbackId, order) =>
        OrderFeedbackUpdate(
          id = Some(orderFeedbackId),
          merchantId = Some(user.merchantId),
          orderId = Some(order.id),
          locationId = order.locationId,
          customerId = order.customerId,
          rating = Gen.chooseNum(0, 5).sample,
          body = Some(randomWords(n = 15, allCapitalized = false)),
          read = Some(genBoolean.instance),
          receivedAt = Some(genZonedDateTimeInThePast.instance),
        )
    }

    orderFeedbackDao.bulkUpsert(orderFeedbacks).extractRecords
  }
}
