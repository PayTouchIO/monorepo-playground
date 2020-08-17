package io.paytouch.core.conversions

import io.paytouch.core.data.model.OrderFeedbackRecord
import io.paytouch.core.entities.{ CustomerMerchant, UserContext, OrderFeedback => OrderFeedbackEntity }

trait OrderFeedbackConversions extends EntityConversion[OrderFeedbackRecord, OrderFeedbackEntity] {

  def fromRecordToEntity(record: OrderFeedbackRecord)(implicit user: UserContext): OrderFeedbackEntity =
    fromRecordAndOptionsToEntity(record, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[OrderFeedbackRecord],
      customerPerOrderFeedback: Option[Map[OrderFeedbackRecord, CustomerMerchant]],
    ) =
    records.map { record =>
      val customer = customerPerOrderFeedback.flatMap(_.get(record))
      fromRecordAndOptionsToEntity(record, customer)
    }

  def fromRecordAndOptionsToEntity(record: OrderFeedbackRecord, customer: Option[CustomerMerchant]) =
    OrderFeedbackEntity(
      id = record.id,
      orderId = record.orderId,
      customerId = record.customerId,
      rating = record.rating,
      body = record.body,
      read = record.read,
      receivedAt = record.receivedAt,
      customer = customer,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )
}
