package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.data.model
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.entities

trait MerchantConversions {

  protected def fromRecordToEntity(
      record: model.MerchantRecord,
    )(implicit
      context: entities.AppContext,
    ): entities.Merchant =
    entities.Merchant.fromRecord(record)

  protected def toUpsertionModel(
      id: UUID,
      update: entities.MerchantUpdate,
    )(implicit
      user: entities.UserContext,
    ): model.MerchantUpdate =
    model.MerchantUpdate(
      id = id,
      urlSlug = update.urlSlug,
      paymentProcessor = None,
      paymentProcessorConfig = None,
    )
}
