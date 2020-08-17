package io.paytouch.core.conversions

import io.paytouch.core.data.model.GiftCardPassTransactionRecord
import io.paytouch.core.data.model.enums.GiftCardPassTransactionType
import io.paytouch.core.entities.{
  GiftCardPassTransaction => GiftCardPassTransactionEntity,
  GiftCardPass => GiftCardPassEntity,
  _,
}

trait GiftCardPassTransactionConversions
    extends EntityConversion[GiftCardPassTransactionRecord, GiftCardPassTransactionEntity] {
  def fromRecordToEntity(
      record: GiftCardPassTransactionRecord,
    )(implicit
      user: UserContext,
    ): GiftCardPassTransactionEntity =
    fromRecordToEntityWithPass(record, None)

  def fromRecordToEntityWithPass(
      record: GiftCardPassTransactionRecord,
      giftCardPass: Option[GiftCardPassEntity],
    )(implicit
      user: UserContext,
    ): GiftCardPassTransactionEntity =
    GiftCardPassTransactionEntity(
      id = record.id,
      total = MonetaryAmount(record.totalAmount.abs),
      `type` =
        if (record.totalAmount < 0)
          GiftCardPassTransactionType.Payment
        else
          GiftCardPassTransactionType.Refund,
      pass = giftCardPass,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )
}
