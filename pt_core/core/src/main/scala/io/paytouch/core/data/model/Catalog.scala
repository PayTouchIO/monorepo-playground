package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.CatalogType

final case class CatalogRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    `type`: CatalogType,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class CatalogUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    name: Option[String],
    `type`: Option[CatalogType],
  ) extends SlickMerchantUpdate[CatalogRecord] {
  def updateRecord(record: CatalogRecord): CatalogRecord =
    CatalogRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      `type` = `type`.getOrElse(record.`type`),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: CatalogRecord = {
    require(merchantId.isDefined, s"Impossible to convert CatalogUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert CatalogUpdate without a name. [$this]")
    require(`type`.isDefined, s"Impossible to convert CatalogUpdate without a type. [$this]")
    CatalogRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      `type` = `type`.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
