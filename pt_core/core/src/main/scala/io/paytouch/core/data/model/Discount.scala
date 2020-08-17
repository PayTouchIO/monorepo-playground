package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.DiscountType

final case class DiscountRecord(
    id: UUID,
    merchantId: UUID,
    title: String,
    `type`: DiscountType,
    amount: BigDecimal,
    requireManagerApproval: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class DiscountUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    title: Option[String],
    `type`: Option[DiscountType],
    amount: Option[BigDecimal],
    requireManagerApproval: Option[Boolean],
  ) extends SlickMerchantUpdate[DiscountRecord] {

  val isPercentage = `type`.contains(DiscountType.Percentage)

  def toRecord: DiscountRecord = {
    require(merchantId.isDefined, s"Impossible to convert DiscountUpdate without a merchant id. [$this]")
    require(title.isDefined, s"Impossible to convert DiscountUpdate without a title. [$this]")
    require(`type`.isDefined, s"Impossible to convert DiscountUpdate without a type. [$this]")
    require(amount.isDefined, s"Impossible to convert DiscountUpdate without an amount. [$this]")
    DiscountRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      title = title.get,
      `type` = `type`.get,
      amount = amount.get,
      requireManagerApproval = requireManagerApproval.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: DiscountRecord): DiscountRecord =
    DiscountRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      title = title.getOrElse(record.title),
      `type` = `type`.getOrElse(record.`type`),
      amount = amount.getOrElse(record.amount),
      requireManagerApproval = requireManagerApproval.getOrElse(record.requireManagerApproval),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
