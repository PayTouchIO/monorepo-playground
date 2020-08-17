package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch._

import io.paytouch.core.data.model.enums.ModifierSetType

final case class ModifierSetRecord(
    id: UUID,
    merchantId: UUID,
    `type`: ModifierSetType,
    name: String,
    optionCount: ModifierOptionCount,
    maximumSingleOptionCount: Option[Int],
    hideOnReceipts: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ModifierSetUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    `type`: Option[ModifierSetType],
    name: Option[String],
    optionCount: Option[ModifierOptionCount],
    maximumSingleOptionCount: Option[Option[Int]],
    hideOnReceipts: Option[Boolean],
  ) extends SlickMerchantUpdate[ModifierSetRecord] {

  def toRecord: ModifierSetRecord = {
    require(merchantId.isDefined, s"Impossible to convert ModifierSetUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert ModifierSetUpdate without a name. [$this]")
    require(hideOnReceipts.isDefined, s"Impossible to convert ModifierSetUpdate without a hideOnReceipts. [$this]")

    ModifierSetRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      `type` = `type`.get,
      name = name.get,
      optionCount = optionCount.getOrElse(ModifierOptionCount.unsafeFromZero),
      maximumSingleOptionCount = maximumSingleOptionCount.getOrElse(None),
      hideOnReceipts = hideOnReceipts.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ModifierSetRecord): ModifierSetRecord =
    ModifierSetRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      `type` = `type`.getOrElse(record.`type`),
      name = name.getOrElse(record.name),
      optionCount = optionCount.getOrElse(record.optionCount),
      maximumSingleOptionCount = maximumSingleOptionCount.getOrElse(record.maximumSingleOptionCount),
      hideOnReceipts = hideOnReceipts.getOrElse(record.hideOnReceipts),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
