package io.paytouch.ordering.entities

import java.util.UUID

import cats.implicits._

import io.paytouch.ordering.data.model.{ MerchantRecord, PaymentProcessorConfig => PaymentProcessorConfigModel }
import io.paytouch.ordering.entities.enums._

final case class Merchant(
    id: UUID,
    urlSlug: String,
    paymentProcessor: PaymentProcessor,
    paymentProcessorConfig: PaymentProcessorConfig,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Merchant
}

object Merchant {
  def fromRecord(record: MerchantRecord) =
    apply(
      id = record.id,
      urlSlug = record.urlSlug,
      paymentProcessor = record.paymentProcessor,
      paymentProcessorConfig = record.paymentProcessorConfig.toEntity,
    )
}

final case class MerchantUpdate(urlSlug: Option[String]) extends UpdateEntity[MerchantUpdate] {
  def asUpsert = this
}
