package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.PassType

final case class GiftCardRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    amounts: Seq[BigDecimal],
    businessName: String,
    templateDetails: Option[String],
    appleWalletTemplateId: Option[String],
    androidPayTemplateId: Option[String],
    active: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickToggleableRecord {
  def templateIdByPassType(passType: PassType) =
    passType match {
      case PassType.Ios     => appleWalletTemplateId
      case PassType.Android => androidPayTemplateId
    }
}

case class GiftCardUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    amounts: Option[Seq[BigDecimal]],
    businessName: Option[String],
    templateDetails: Option[String],
    appleWalletTemplateId: Option[String],
    androidPayTemplateId: Option[String],
    active: Option[Boolean],
  ) extends SlickMerchantUpdate[GiftCardRecord] {
  def updateRecord(record: GiftCardRecord): GiftCardRecord =
    GiftCardRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      amounts = amounts.getOrElse(record.amounts),
      businessName = businessName.getOrElse(record.businessName),
      templateDetails = templateDetails.orElse(record.templateDetails),
      appleWalletTemplateId = appleWalletTemplateId.orElse(record.appleWalletTemplateId),
      androidPayTemplateId = androidPayTemplateId.orElse(record.androidPayTemplateId),
      active = active.getOrElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: GiftCardRecord = {
    require(merchantId.isDefined, s"Impossible to convert GiftCardUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert GiftCardUpdate without a product id. [$this]")
    require(businessName.isDefined, s"Impossible to convert GiftCardUpdate without a business name. [$this]")

    GiftCardRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      amounts = amounts.getOrElse(Seq.empty),
      businessName = businessName.get,
      templateDetails = templateDetails,
      appleWalletTemplateId = appleWalletTemplateId,
      androidPayTemplateId = androidPayTemplateId,
      active = active.getOrElse(true),
      createdAt = now,
      updatedAt = now,
    )
  }
}

object GiftCardUpdate {
  def empty =
    GiftCardUpdate(
      id = None,
      merchantId = None,
      productId = None,
      amounts = None,
      businessName = None,
      templateDetails = None,
      appleWalletTemplateId = None,
      androidPayTemplateId = None,
      active = None,
    )
}
