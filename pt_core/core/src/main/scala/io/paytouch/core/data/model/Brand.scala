package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class BrandRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class BrandUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    name: Option[String],
  ) extends SlickMerchantUpdate[BrandRecord] {

  def updateRecord(record: BrandRecord): BrandRecord =
    BrandRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: BrandRecord = {
    require(merchantId.isDefined, s"Impossible to convert BrandUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert BrandUpdate without a name. [$this]")
    BrandRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
