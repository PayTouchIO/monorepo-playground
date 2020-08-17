package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.entities.ResettableUUID
import io.paytouch.core.entities.enums.HandledVia

final case class TipsAssignmentRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    userId: Option[UUID],
    orderId: Option[UUID],
    amount: BigDecimal,
    handledVia: HandledVia,
    handledViaCashDrawerActivityId: Option[UUID],
    cashDrawerActivityId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    assignedAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteRecord

case class TipsAssignmentUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    userId: ResettableUUID,
    orderId: ResettableUUID,
    amount: Option[BigDecimal],
    handledVia: Option[HandledVia],
    handledViaCashDrawerActivityId: ResettableUUID,
    cashDrawerActivityId: ResettableUUID,
    paymentType: Option[TransactionPaymentType],
    assignedAt: Option[ZonedDateTime],
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[TipsAssignmentRecord] {
  def toRecord: TipsAssignmentRecord = {
    require(merchantId.isDefined, s"Impossible to convert TipsAssignmentUpdate without a merchant id. [$this]")
    require(locationId.isDefined, s"Impossible to convert TipsAssignmentUpdate without a location id. [$this]")
    require(amount.isDefined, s"Impossible to convert TipsAssignmentUpdate without a amount. [$this]")
    require(handledVia.isDefined, s"Impossible to convert TipsAssignmentUpdate without a handledVia. [$this]")
    require(assignedAt.isDefined, s"Impossible to convert TipsAssignmentUpdate without a assigned at. [$this]")

    TipsAssignmentRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId.get,
      userId = userId,
      orderId = orderId,
      amount = amount.get,
      handledVia = handledVia.get,
      handledViaCashDrawerActivityId = handledViaCashDrawerActivityId,
      cashDrawerActivityId = cashDrawerActivityId,
      paymentType = paymentType,
      assignedAt = assignedAt.get,
      createdAt = now,
      updatedAt = now,
      deletedAt = deletedAt,
    )
  }

  def updateRecord(record: TipsAssignmentRecord): TipsAssignmentRecord =
    TipsAssignmentRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      orderId = orderId.getOrElse(record.orderId),
      amount = amount.getOrElse(record.amount),
      handledVia = handledVia.getOrElse(record.handledVia),
      handledViaCashDrawerActivityId = handledViaCashDrawerActivityId.getOrElse(record.handledViaCashDrawerActivityId),
      cashDrawerActivityId = cashDrawerActivityId.getOrElse(record.cashDrawerActivityId),
      paymentType = paymentType.orElse(record.paymentType),
      assignedAt = assignedAt.getOrElse(record.assignedAt),
      createdAt = record.createdAt,
      updatedAt = now,
      deletedAt = deletedAt.orElse(record.deletedAt),
    )
}
