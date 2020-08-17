package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.CommentRecord
import io.paytouch.core.data.model.enums.CommentType

class CommentsTable(tag: Tag) extends SlickMerchantTable[CommentRecord](tag, "comments") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def userId = column[UUID]("user_id")
  def objectId = column[UUID]("object_id")
  def objectType = column[CommentType]("object_type")
  def body = column[String]("body")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      userId,
      objectId,
      objectType,
      body,
      createdAt,
      updatedAt,
    ).<>(CommentRecord.tupled, CommentRecord.unapply)

}
