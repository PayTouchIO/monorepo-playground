package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.json.JsonSupport

final case class PaymentProcessorCallbackRecord(
    id: UUID,
    paymentProcessor: PaymentProcessor,
    status: PaymentProcessorCallbackStatus,
    reference: Option[String],
    payload: JsonSupport.JValue,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

final case class PaymentProcessorCallbackUpdate(
    id: Default[UUID],
    paymentProcessor: Option[PaymentProcessor],
    status: Option[PaymentProcessorCallbackStatus],
    reference: ResettableString,
    payload: Option[JsonSupport.JValue],
  ) extends SlickUpdate[PaymentProcessorCallbackRecord] {

  def toRecord: PaymentProcessorCallbackRecord = {
    requires("payment_processor" -> paymentProcessor)
    requires("status" -> status)
    requires("payload" -> payload)
    PaymentProcessorCallbackRecord(
      id = id.getOrDefault,
      paymentProcessor = paymentProcessor.get,
      status = status.get,
      reference = reference,
      payload = payload.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: PaymentProcessorCallbackRecord): PaymentProcessorCallbackRecord =
    PaymentProcessorCallbackRecord(
      id = id.getOrElse(record.id),
      paymentProcessor = paymentProcessor.getOrElse(record.paymentProcessor),
      status = status.getOrElse(record.status),
      reference = reference.getOrElse(record.reference),
      payload = payload.getOrElse(record.payload),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
