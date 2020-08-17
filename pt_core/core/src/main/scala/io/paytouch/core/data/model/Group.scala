package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class GroupRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class GroupUpdate(
    id: Option[UUID] = None,
    merchantId: Option[UUID],
    name: Option[String],
  ) extends SlickMerchantUpdate[GroupRecord] {

  def updateRecord(record: GroupRecord): GroupRecord =
    GroupRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: GroupRecord = {
    require(merchantId.isDefined, s"Impossible to convert GroupUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert GroupUpdate without a name. [$this]")
    GroupRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
