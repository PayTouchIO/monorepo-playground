package io.paytouch.core.resources.comments

import java.util.UUID

import io.paytouch.core.data.model.{ CommentRecord, UserRecord }
import io.paytouch.core.entities.{ CommentCreation, CommentUpdate, Comment => CommentEntity }
import io.paytouch.core.utils.FSpec

trait CommentResponseAssertions { self: FSpec =>

  val commentDao = daos.commentDao

  def assertCommentCreation(commentId: UUID, creation: CommentCreation) =
    assertCommentUpdate(commentId, creation.asUpdate)

  def assertCommentUpdate(commentId: UUID, update: CommentUpdate) = {
    val comment = commentDao.findById(commentId).await.get
    if (update.objectId.isDefined) update.objectId ==== Some(comment.objectId)
    if (update.body.isDefined) update.body ==== Some(comment.body)
  }

  def assertCommentResponse(
      entity: CommentEntity,
      record: CommentRecord,
      user: UserRecord,
    ) = {
    entity.id ==== record.id
    entity.user.id ==== user.id
    entity.body ==== record.body
    entity.createdAt ==== record.createdAt
  }

  def assertCommentResponseById(
      recordId: UUID,
      entity: CommentEntity,
      user: UserRecord,
    ) = {
    val comment = commentDao.findById(recordId).await.get
    assertCommentResponse(entity, comment, user)
  }

  def assertCommentDeleted(commentId: UUID) = afterAWhile(commentDao.findById(commentId).await must beNone)

  def assertCommentWasntDeleted(commentId: UUID) = commentDao.findById(commentId).await must beSome
}
