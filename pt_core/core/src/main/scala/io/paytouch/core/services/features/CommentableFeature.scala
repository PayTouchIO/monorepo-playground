package io.paytouch.core.services.features

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.CommentFilters
import io.paytouch.core.services.CommentService
import io.paytouch.core.utils.FindResult._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.{ Implicits, Multiple }
import io.paytouch.core.validators.features.ValidatorWithExtraFields

import scala.concurrent._

trait CommentableFeature extends Implicits { self =>
  type Record <: SlickMerchantRecord
  type Validator <: ValidatorWithExtraFields[Record]

  protected def validator: Validator

  def commentService: CommentService
  def commentType: CommentType

  def createComment(
      commentId: UUID,
      creation: CommentCreation,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Comment]]] =
    for {
      validObjectId <- validator.accessOneById(creation.objectId)
      creationResult <- commentService.create(commentId, creation.copy(objectType = Some(commentType)))
    } yield Multiple.combine(validObjectId, creationResult) { case (_, result) => result }

  def deleteComment(commentId: UUID)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    for {
      validComment <- commentService.accessOneById(commentId)
      validObjectId <- validator.accessOneByOptId(validComment.toOption.map(_.objectId))
    } yield Multiple.combine(validComment, validObjectId) {
      case (_, _) => commentService.bulkDelete(Seq(commentId))
    }

  def listComments(
      objectId: UUID,
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): Future[ErrorsOr[FindResult[Comment]]] =
    validator.accessOneById(objectId).flatMapTraverse { _ =>
      commentService.findAll(CommentFilters(objectId = Some(objectId), `type` = Some(commentType)))(NoExpansions())
    }

  def updateComment(
      commentId: UUID,
      update: CommentUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Comment]]] =
    for {
      validComment <- commentService.accessOneById(commentId)
      objectId = update.objectId.orElse(validComment.toOption.map(_.objectId))
      validObjectId <- validator.accessOneByOptId(objectId)
      updateResult <- commentService.update(commentId, update.copy(objectType = Some(commentType)))
    } yield Multiple.combine(validComment, validObjectId, updateResult) { case (_, _, result) => result }
}
