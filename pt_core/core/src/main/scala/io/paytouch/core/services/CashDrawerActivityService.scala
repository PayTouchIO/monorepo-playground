package io.paytouch.core.services

import java.util.UUID

import cats.data.OptionT
import cats.instances.future._
import io.paytouch.core.conversions.CashDrawerActivityConversions
import io.paytouch.core.data.daos.{ CashDrawerActivityDao, Daos }
import io.paytouch.core.data.model.{
  CashDrawerActivityRecord,
  CashDrawerActivityUpdate => CashDrawerActivityUpdateModel,
}
import io.paytouch.core.entities.{
  CashDrawerActivity => CashDrawerActivityEntity,
  CashDrawerActivityUpsertion => CashDrawerActivityUpsertionEntity,
  _,
}
import io.paytouch.core.expansions.{ CashDrawerActivityExpansions, NoExpansions }
import io.paytouch.core.filters.CashDrawerActivityFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features.FindAllFeature
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.ResultType
import io.paytouch.core.validators.{ CashDrawerActivityRecoveryValidator, CashDrawerRecoveryValidator }

import scala.concurrent._

class CashDrawerActivityService(
    val cashDrawerService: CashDrawerService,
    val userService: UserService,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends CashDrawerActivityConversions
       with FindAllFeature {

  type Dao = CashDrawerActivityDao
  type Filters = CashDrawerActivityFilters
  type Entity = CashDrawerActivityEntity
  type Expansions = CashDrawerActivityExpansions
  type Record = CashDrawerActivityRecord

  protected val dao = daos.cashDrawerActivityDao

  val defaultExpansions = CashDrawerActivityExpansions(withUserInfo = false)
  val recoveryValidator = new CashDrawerActivityRecoveryValidator(new CashDrawerRecoveryValidator)

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    getOptionalUserInfo(records)(e.withUserInfo).map { userInfoByIds =>
      fromRecordsAndOptionsToEntities(records, userInfoByIds)
    }

  private def getOptionalUserInfo(
      records: Seq[Record],
    )(
      withUserInfo: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, UserInfo]]] =
    if (withUserInfo) userService.getUserInfoByIds(records.flatMap(_.userId).distinct).map { userInfos =>
      Some(userInfos.map(u => (u.id, u)).toMap)
    }
    else Future.successful(None)

  def syncById(
      id: UUID,
      upsertion: CashDrawerActivityUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    recoverUpsertionModel(id, upsertion).flatMap { updates =>
      for {
        (resultType, record) <- dao.upsert(updates)
        entity = fromRecordAndOptionsToEntity(record, None)
        _ <- sendSyncedMessages(entity).value
      } yield (resultType, entity)
    }

  private def recoverUpsertionModel(
      id: UUID,
      upsertion: CashDrawerActivityUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[CashDrawerActivityUpdateModel] =
    recoveryValidator.recoverUpsertion(id, upsertion)

  private def findLocationIdByCashDrawerId(cashDrawerId: Option[UUID]) =
    for {
      cashDrawerId <- OptionT.fromOption[Future](cashDrawerId)
      cashDrawer <- OptionT(cashDrawerService.findRecordById(cashDrawerId))
      locationId <- OptionT.fromOption(cashDrawer.locationId)
    } yield locationId

  private def sendSyncedMessages(entity: Entity)(implicit user: UserContext) =
    findLocationIdByCashDrawerId(entity.cashDrawerId).map { locationId =>
      messageHandler.sendEntitySynced(entity, locationId)
    }
}
