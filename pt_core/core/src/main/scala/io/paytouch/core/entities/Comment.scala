package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.entities.enums.ExposedName

final case class Comment(
    id: UUID,
    user: UserInfo,
    body: String,
    createdAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Comment
}

final case class CommentCreation(
    objectId: UUID,
    body: String,
    objectType: Option[CommentType] = None,
  ) extends CreationEntity[Comment, CommentUpdate] {
  def asUpdate =
    CommentUpdate(objectId = Some(objectId), body = Some(body), objectType = objectType)
}

final case class CommentUpdate(
    objectId: Option[UUID],
    body: Option[String],
    objectType: Option[CommentType],
  ) extends UpdateEntity[Comment]
