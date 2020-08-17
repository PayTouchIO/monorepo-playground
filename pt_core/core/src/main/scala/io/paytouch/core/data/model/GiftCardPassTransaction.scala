package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class GiftCardPassTransactionRecord(
    id: UUID,
    merchantId: UUID,
    giftCardPassId: UUID,
    totalAmount: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class GiftCardPassTransactionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    giftCardPassId: Option[UUID],
    totalAmount: Option[BigDecimal],
  ) extends SlickMerchantUpdate[GiftCardPassTransactionRecord] {

  def updateRecord(record: GiftCardPassTransactionRecord): GiftCardPassTransactionRecord =
    GiftCardPassTransactionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      giftCardPassId = giftCardPassId.getOrElse(record.giftCardPassId),
      totalAmount = totalAmount.getOrElse(record.totalAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: GiftCardPassTransactionRecord = {
    require(merchantId.isDefined, s"Impossible to convert GiftCardPassTransactionUpdate without a merchant id. [$this]")
    require(
      giftCardPassId.isDefined,
      s"Impossible to convert GiftCardPassTransactionUpdate without a gift card pass id. [$this]",
    )
    require(
      totalAmount.isDefined,
      s"Impossible to convert GiftCardPassTransactionUpdate without a total amount. [$this]",
    )
    GiftCardPassTransactionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      giftCardPassId = giftCardPassId.get,
      totalAmount = totalAmount.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
