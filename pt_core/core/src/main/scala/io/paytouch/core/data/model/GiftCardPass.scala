package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch._

final case class GiftCardPassRecord(
    id: UUID,
    merchantId: UUID,
    lookupId: String,
    giftCardId: UUID,
    orderItemId: UUID,
    originalAmount: BigDecimal,
    balanceAmount: BigDecimal,
    iosPassPublicUrl: Option[String],
    androidPassPublicUrl: Option[String],
    isCustomAmount: Boolean,
    passInstalledAt: Option[ZonedDateTime],
    recipientEmail: Option[String],
    onlineCode: GiftCardPass.OnlineCode,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class GiftCardPassUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    lookupId: Option[String],
    giftCardId: Option[UUID],
    orderItemId: Option[UUID],
    originalAmount: Option[BigDecimal],
    balanceAmount: Option[BigDecimal],
    iosPassPublicUrl: Option[String],
    androidPassPublicUrl: Option[String],
    isCustomAmount: Option[Boolean],
    passInstalledAt: Option[ZonedDateTime],
    recipientEmail: Option[String],
    onlineCode: Option[GiftCardPass.OnlineCode],
  ) extends SlickMerchantUpdate[GiftCardPassRecord] {
  def updateRecord(record: GiftCardPassRecord): GiftCardPassRecord =
    GiftCardPassRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      lookupId = lookupId.getOrElse(record.lookupId),
      giftCardId = giftCardId.getOrElse(record.giftCardId),
      orderItemId = orderItemId.getOrElse(record.orderItemId),
      originalAmount = originalAmount.getOrElse(record.originalAmount),
      balanceAmount = balanceAmount.getOrElse(record.balanceAmount),
      iosPassPublicUrl = iosPassPublicUrl.orElse(record.iosPassPublicUrl),
      androidPassPublicUrl = androidPassPublicUrl.orElse(record.androidPassPublicUrl),
      isCustomAmount = record.isCustomAmount, // cannot be changed
      passInstalledAt = passInstalledAt.orElse(record.passInstalledAt),
      recipientEmail = recipientEmail.orElse(record.recipientEmail),
      onlineCode = record.onlineCode, // cannot be changed
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: GiftCardPassRecord = {
    require(merchantId.isDefined, s"Impossible to convert GiftCardPassUpdate without a merchant id. [$this]")
    require(lookupId.isDefined, s"Impossible to convert GiftCardPassUpdate without a lookup id. [$this]")
    require(giftCardId.isDefined, s"Impossible to convert GiftCardPassUpdate without a gift card id. [$this]")
    require(orderItemId.isDefined, s"Impossible to convert GiftCardPassUpdate without a order item id. [$this]")
    require(originalAmount.isDefined, s"Impossible to convert GiftCardPassUpdate without an original amount. [$this]")
    require(balanceAmount.isDefined, s"Impossible to convert GiftCardPassUpdate without a balance amount. [$this]")
    require(onlineCode.isDefined, s"Impossible to convert GiftCardPassUpdate without an onlineCode. [$this]")

    GiftCardPassRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      lookupId = lookupId.get,
      giftCardId = giftCardId.get,
      orderItemId = orderItemId.get,
      originalAmount = originalAmount.get,
      balanceAmount = balanceAmount.get,
      iosPassPublicUrl = iosPassPublicUrl,
      androidPassPublicUrl = androidPassPublicUrl,
      isCustomAmount = isCustomAmount.getOrElse(false),
      passInstalledAt = passInstalledAt,
      recipientEmail = recipientEmail,
      onlineCode = onlineCode.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
