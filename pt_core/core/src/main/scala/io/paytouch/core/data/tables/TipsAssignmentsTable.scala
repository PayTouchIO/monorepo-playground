package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.data.model.TipsAssignmentRecord
import io.paytouch.core.entities.enums.HandledVia

class TipsAssignmentsTable(tag: Tag) extends SlickSoftDeleteTable[TipsAssignmentRecord](tag, "tips_assignments") {
  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")
  def userId = column[Option[UUID]]("user_id")
  def orderId = column[Option[UUID]]("order_id")
  def amount = column[BigDecimal]("amount")
  def handledVia = column[HandledVia]("handled_via")
  def handledViaCashDrawerActivityId = column[Option[UUID]]("handled_via_cash_drawer_activity_id")
  def cashDrawerActivityId = column[Option[UUID]]("cash_drawer_activity_id")
  def paymentType = column[Option[TransactionPaymentType]]("payment_type")
  def assignedAt = column[ZonedDateTime]("assigned_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")

  def * =
    (
      id,
      merchantId,
      locationId,
      userId,
      orderId,
      amount,
      handledVia,
      handledViaCashDrawerActivityId,
      cashDrawerActivityId,
      paymentType,
      assignedAt,
      createdAt,
      updatedAt,
      deletedAt,
    ).<>(TipsAssignmentRecord.tupled, TipsAssignmentRecord.unapply)

}
