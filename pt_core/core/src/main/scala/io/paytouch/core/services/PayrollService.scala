package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.conversions.PayrollConversions
import io.paytouch.core.data.daos.{ Daos, PayrollDao }
import io.paytouch.core.data.model.{ TimeCardTotals, UserRecord }
import io.paytouch.core.entities.{ MonetaryAmount, UserContext, Payroll => PayrollEntity }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.PayrollFilters
import io.paytouch.core.services.features._

class PayrollService(val userService: UserService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends PayrollConversions
       with FindAllFeature {
  type Dao = PayrollDao
  type Entity = PayrollEntity
  type Expansions = NoExpansions
  type Filters = PayrollFilters
  type Record = UserRecord

  protected val dao = daos.payrollDao
  val orderDao = daos.orderDao
  val timeCardDao = daos.timeCardDao
  val defaultFilters = PayrollFilters()

  def enrich(
      records: Seq[Record],
      f: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] = {
    val payrollTotalsR = getMinuteTotalsPerUser(records, f)
    val tipsTotalsR = getTotalTipsPerUser(records, f)
    for {
      payrollTotals <- payrollTotalsR
      tipsTotals <- tipsTotalsR
    } yield fromRecordsAndOptionsToEntities(records, payrollTotals, tipsTotals)
  }

  private def getMinuteTotalsPerUser(
      records: Seq[Record],
      f: Filters,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, TimeCardTotals]] = {
    val userIds = records.map(_.id)
    timeCardDao.findMinuteTotalsByUserIds(userIds, f.locationIds, f.from, f.to)
  }

  private def getTotalTipsPerUser(
      records: Seq[Record],
      f: Filters,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[MonetaryAmount]]] = {
    val userIds = records.map(_.id)
    orderDao.getTotalTipsPerUser(userIds, f.locationIds, f.from, f.to).map { result =>
      result.transform((_, amount) => Seq(MonetaryAmount(amount)))
    }
  }
}
