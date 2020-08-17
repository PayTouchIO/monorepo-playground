package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities.{ PaymentProcessorConfig => _, _ }
import io.paytouch.ordering.entities.enums.PaymentProcessor

final case class MerchantRecord(
    id: UUID,
    urlSlug: String,
    paymentProcessor: PaymentProcessor,
    paymentProcessorConfig: PaymentProcessorConfig,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

final case class MerchantUpdate(
    id: Default[UUID],
    urlSlug: Option[String],
    paymentProcessor: Option[PaymentProcessor],
    paymentProcessorConfig: Option[PaymentProcessorConfig],
  ) extends SlickUpdate[MerchantRecord] {

  def toRecord: MerchantRecord = {
    requires(
      "url slug" -> urlSlug,
      "payment processor" -> paymentProcessor,
      "payment processor config" -> paymentProcessorConfig,
    )

    MerchantRecord(
      id = id.getOrDefault,
      urlSlug = urlSlug.get,
      paymentProcessor = paymentProcessor.get,
      paymentProcessorConfig = paymentProcessorConfig.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: MerchantRecord): MerchantRecord =
    MerchantRecord(
      id = id.getOrElse(record.id),
      urlSlug = urlSlug.getOrElse(record.urlSlug),
      paymentProcessor = paymentProcessor.getOrElse(record.paymentProcessor),
      paymentProcessorConfig = paymentProcessorConfig.getOrElse(record.paymentProcessorConfig),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
