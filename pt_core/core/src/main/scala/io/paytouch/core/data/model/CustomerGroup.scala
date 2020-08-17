package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class CustomerGroupRecord(
    id: UUID,
    merchantId: UUID,
    customerId: UUID,
    groupId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class CustomerGroupUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    customerId: Option[UUID],
    groupId: Option[UUID],
  ) extends SlickMerchantUpdate[CustomerGroupRecord] {

  def toRecord: CustomerGroupRecord = {
    require(merchantId.isDefined, s"Impossible to convert CustomerGroupUpdate without a merchant id. [$this]")
    require(customerId.isDefined, s"Impossible to convert CustomerGroupUpdate without a customer id. [$this]")
    require(groupId.isDefined, s"Impossible to convert CustomerGroupUpdate without a group id. [$this]")
    CustomerGroupRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      customerId = customerId.get,
      groupId = groupId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CustomerGroupRecord): CustomerGroupRecord =
    CustomerGroupRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      customerId = customerId.getOrElse(record.customerId),
      groupId = groupId.getOrElse(record.groupId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
