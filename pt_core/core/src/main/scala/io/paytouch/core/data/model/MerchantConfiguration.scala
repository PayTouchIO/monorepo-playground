package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class MerchantConfigurationRecord(
    id: UUID,
    merchantId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class MerchantConfigurationUpdate(id: Option[UUID], merchantId: Option[UUID])
    extends SlickMerchantUpdate[MerchantConfigurationRecord] {

  def toRecord: MerchantConfigurationRecord = {
    require(merchantId.isDefined, s"Impossible to convert MerchantConfigurationUpdate without a merchant id. [$this]")
    MerchantConfigurationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: MerchantConfigurationRecord): MerchantConfigurationRecord =
    MerchantConfigurationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
