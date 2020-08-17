package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ ExposedEntity, UserContext }

trait SQSMessage[T] {

  def eventName: String
  def payload: EntityPayloadLike[T]
}

trait EntityPayloadLike[T] {
  def `object`: ExposedName
  def data: T
  def merchantId: UUID
}

final case class EntityPayload[T](
    `object`: ExposedName,
    data: T,
    merchantId: UUID,
    locationId: Option[UUID],
    pusherSocketId: Option[String],
  ) extends EntityPayloadLike[T]

object EntityPayload {

  def apply[T <: ExposedEntity](data: T, locationId: Option[UUID])(implicit user: UserContext): EntityPayload[T] =
    apply(data, user.merchantId, locationId, user.pusherSocketId)

  def apply[T <: ExposedEntity](
      data: T,
      merchantId: UUID,
      locationId: Option[UUID],
      pusherSocketId: Option[String],
    ): EntityPayload[T] =
    apply(data.classShortName, data, merchantId, locationId, pusherSocketId)
}

trait EmailEntityPayloadLike[T] extends EntityPayloadLike[T] {
  def recipientEmail: String
}

final case class EmailEntityPayload[T](
    `object`: ExposedName,
    recipientEmail: String,
    data: T,
    merchantId: UUID,
  ) extends EmailEntityPayloadLike[T]

object EmailEntityPayload {

  def apply[T <: ExposedEntity](
      recipientEmail: String,
      data: T,
      merchantId: UUID,
    ): EmailEntityPayload[T] =
    apply(data.classShortName, recipientEmail, data, merchantId)
}

trait PtNotifierMsg[T] extends SQSMessage[T]

trait PtCoreMsg[T] extends SQSMessage[T]

trait PtDeliveryMsg[T] extends SQSMessage[T]

trait PtOrderingMsg[T] extends SQSMessage[T]
