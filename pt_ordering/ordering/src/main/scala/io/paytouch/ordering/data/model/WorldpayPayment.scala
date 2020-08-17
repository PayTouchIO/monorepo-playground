package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.model.Uri

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import io.paytouch.ordering.entities.enums.EnumEntrySnake
import enumeratum._

sealed trait WorldpayPaymentType extends EnumEntrySnake

case object WorldpayPaymentType extends Enum[WorldpayPaymentType] {

  case object Cart extends WorldpayPaymentType
  case object PaymentIntent extends WorldpayPaymentType

  val values = findValues
}

final case class WorldpayPaymentRecord(
    id: UUID,
    objectId: UUID,
    objectType: WorldpayPaymentType,
    transactionSetupId: String,
    successReturnUrl: Uri,
    failureReturnUrl: Uri,
    status: WorldpayPaymentStatus,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickRecord

final case class WorldpayPaymentUpdate(
    id: Default[UUID],
    objectId: Option[UUID] = None,
    objectType: Option[WorldpayPaymentType] = None,
    transactionSetupId: Option[String] = None,
    successReturnUrl: Option[Uri] = None,
    failureReturnUrl: Option[Uri] = None,
    status: Option[WorldpayPaymentStatus],
  ) extends SlickUpdate[WorldpayPaymentRecord] {

  def toRecord: WorldpayPaymentRecord = {
    requires(
      "object_id" -> objectId,
      "object_type" -> objectType,
      "transaction_setup_id" -> transactionSetupId,
      "success_return_url" -> successReturnUrl,
      "failure_return_url" -> failureReturnUrl,
      "status" -> status,
    )

    WorldpayPaymentRecord(
      id = id.getOrDefault,
      objectId = objectId.get,
      objectType = objectType.get,
      transactionSetupId = transactionSetupId.get,
      successReturnUrl = successReturnUrl.get,
      failureReturnUrl = failureReturnUrl.get,
      status = status.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: WorldpayPaymentRecord): WorldpayPaymentRecord =
    WorldpayPaymentRecord(
      id = record.id,
      objectId = record.objectId,
      objectType = record.objectType,
      transactionSetupId = record.transactionSetupId,
      successReturnUrl = record.successReturnUrl,
      failureReturnUrl = record.failureReturnUrl,
      status = status.getOrElse(record.status),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
