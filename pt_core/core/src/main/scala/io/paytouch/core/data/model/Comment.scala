package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.data.model.upsertions.UpsertionModel

final case class CommentRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    objectId: UUID,
    objectType: CommentType,
    body: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class CommentUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: Option[UUID],
    objectId: Option[UUID],
    objectType: Option[CommentType],
    body: Option[String],
  ) extends SlickMerchantUpdate[CommentRecord]
       with UpsertionModel[CommentRecord] {

  def toRecord: CommentRecord = {
    require(merchantId.isDefined, s"Impossible to convert CommentUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert CommentUpdate without a user id. [$this]")
    require(objectId.isDefined, s"Impossible to convert CommentUpdate without a object id. [$this]")
    require(objectType.isDefined, s"Impossible to convert CommentUpdate without a object type. [$this]")
    require(body.isDefined, s"Impossible to convert CommentUpdate without a body. [$this]")
    CommentRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      objectId = objectId.get,
      objectType = objectType.get,
      body = body.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CommentRecord): CommentRecord =
    CommentRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      objectId = objectId.getOrElse(record.objectId),
      objectType = objectType.getOrElse(record.objectType),
      body = body.getOrElse(record.body),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
