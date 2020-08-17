package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.CommentConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.{ CommentRecord, CommentUpdate => CommentUpdateModel }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ Comment => CommentEntity, CommentUpdate => CommentUpdateEntity, _ }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.CommentFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.CommentValidator
import io.paytouch.core.withTag

import scala.concurrent._

class CommentService(
    val eventTracker: ActorRef withTag EventTracker,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends CommentConversions
       with DeleteFeature
       with CreateAndUpdateFeature
       with FindAllFeature
       with FindByIdFeature {

  type Creation = CommentCreation
  type Dao = CommentDao
  type Entity = CommentEntity
  type Expansions = NoExpansions
  type Filters = CommentFilters
  type Model = CommentUpdateModel
  type Record = CommentRecord
  type Update = CommentUpdateEntity
  type Validator = CommentValidator

  protected val dao = daos.commentDao
  protected val validator = new CommentValidator
  val defaultFilters = CommentFilters()

  val classShortName = ExposedName.Comment

  def accessOneById(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Record]] =
    validator.accessOneById(id)

  protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    Future.successful(Multiple.success(fromUpsertionToUpdate(id, upsertion)))

  def enrich(comments: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      userInfos <- getUsersByComment(comments)
    } yield fromRecordsAndOptionsToEntities(comments, userInfos)

  private def getUsersByComment(records: Seq[Record]): Future[Map[Record, UserInfo]] =
    getRelatedField[UserInfo](userService.getUserInfoByIds, _.id, _.userId, records)

  override implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(NoExpansions())
    } yield (resultType, enrichedRecord)

}
