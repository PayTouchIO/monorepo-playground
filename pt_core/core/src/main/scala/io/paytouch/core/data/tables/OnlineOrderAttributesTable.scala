package io.paytouch.core.data.tables

import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.OnlineOrderAttributeRecord
import io.paytouch.core.data.model.enums.{ AcceptanceStatus, CancellationStatus }

class OnlineOrderAttributesTable(tag: Tag)
    extends SlickMerchantTable[OnlineOrderAttributeRecord](tag, "online_order_attributes") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def acceptanceStatus = column[AcceptanceStatus]("acceptance_status")
  def rejectionReason = column[Option[String]]("rejection_reason")
  def prepareByTime = column[Option[LocalTime]]("prepare_by_time")
  def prepareByDateTime = column[Option[ZonedDateTime]]("prepare_by_date_time")
  def estimatedPrepTimeInMins = column[Option[Int]]("estimated_prep_time_in_mins")
  def acceptedAt = column[Option[ZonedDateTime]]("accepted_at")
  def rejectedAt = column[Option[ZonedDateTime]]("rejected_at")
  def estimatedReadyAt = column[Option[ZonedDateTime]]("estimated_ready_at")
  def estimatedDeliveredAt = column[Option[ZonedDateTime]]("estimated_delivered_at")
  def cancellationStatus = column[Option[CancellationStatus]]("cancellation_status")
  def cancellationReason = column[Option[String]]("cancellation_reason")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      acceptanceStatus,
      rejectionReason,
      prepareByTime,
      prepareByDateTime,
      estimatedPrepTimeInMins,
      acceptedAt,
      rejectedAt,
      estimatedReadyAt,
      estimatedDeliveredAt,
      cancellationStatus,
      cancellationReason,
      createdAt,
      updatedAt,
    ).<>(OnlineOrderAttributeRecord.tupled, OnlineOrderAttributeRecord.unapply)
}
