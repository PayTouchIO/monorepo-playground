package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import io.paytouch.core._
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.TipsAssignmentConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos.{ Daos, TipsAssignmentDao }
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.TipsAssignmentFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features._
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.ResultType
import io.paytouch.core.validators._

class TipsAssignmentService(
    val eventTracker: ActorRef withTag EventTracker,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends TipsAssignmentConversions
       with FindAllFeature
       with SoftDeleteFeature {
  type Dao = TipsAssignmentDao
  type Entity = TipsAssignment
  type Expansions = NoExpansions
  type Filters = TipsAssignmentFilters
  type Record = TipsAssignmentRecord
  type Validator = TipsAssignmentValidator

  protected val dao = daos.tipsAssignmentDao
  protected val validator = new TipsAssignmentValidator

  final override val classShortName: ExposedName =
    ExposedName.TipsAssignment

  val defaultFilters = TipsAssignmentFilters()
  val defaultExpansions = NoExpansions()
  val recoveryValidator = new TipsAssignmentRecoveryValidator

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    Future.successful {
      fromRecordsToEntities(records)
    }

  def findByOrderIds(orderIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    dao.findByOrderIds(orderIds).map(records => toSeqEntity(records).groupBy(_.orderId.get))

  def syncById(
      id: UUID,
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    recoverUpsertionModel(id, upsertion).flatMap { updates =>
      for {
        (resultType, record) <- dao.upsert(updates)
        entity = fromRecordToEntity(record)
        _ <- sendSyncedMessages(entity)
      } yield (resultType, entity)
    }

  private def recoverUpsertionModel(
      id: UUID,
      upsertion: TipsAssignmentUpsertion,
    )(implicit
      user: UserContext,
    ): Future[TipsAssignmentUpdate] =
    recoveryValidator.recoverUpsertion(id, upsertion)

  private def sendSyncedMessages(entity: Entity)(implicit user: UserContext) =
    Future.successful {
      messageHandler.sendEntitySynced(entity, entity.locationId)
    }
}
