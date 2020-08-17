package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickCommonDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums.{ NextNumberType, ScopeType }
import io.paytouch.core.data.tables.NextNumbersTable
import io.paytouch.core.entities.{ Scope, ScopeKey }
import io.paytouch.core.utils.UtcTime

import scala.concurrent._

class NextNumberDao(
    val locationDao: LocationDao,
    val locationSettingsDao: LocationSettingsDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickCommonDao {

  type Record = NextNumberRecord
  type Table = NextNumbersTable

  val table = TableQuery[Table]

  private val initialNumb = 1

  def queryNextPurchaseOrderNumberForMerchantId(merchantId: UUID) =
    queryNextNumber(Scope.fromMerchantId(merchantId), NextNumberType.PurchaseOrder)

  def queryNextReceivingOrderNumberForMerchantId(merchantId: UUID) =
    queryNextNumber(Scope.fromMerchantId(merchantId), NextNumberType.ReceivingOrder)

  def queryNextTransferOrderNumberForMerchantId(merchantId: UUID) =
    queryNextNumber(Scope.fromMerchantId(merchantId), NextNumberType.TransferOrder)

  def queryNextReturnOrderNumberForMerchantId(merchantId: UUID) =
    queryNextNumber(Scope.fromMerchantId(merchantId), NextNumberType.ReturnOrder)

  def queryNextOrderNumberForLocationId(locationId: UUID) =
    for {
      scope <- loadScopeKey(locationId)
      nextNumber <- queryNextNumber(scope, NextNumberType.Order)
    } yield nextNumber

  private def loadScopeKey(locationId: UUID): DBIOAction[Scope, NoStream, Effect.Read] = {
    val query = for {
      maybeLocation <- locationDao.queryById(locationId)
      maybeLocationSettings <- locationSettingsDao.queryFindOneByLocationId(locationId)
    } yield (maybeLocation, maybeLocationSettings)

    query.result.headOption.map {
      case Some((location, locationSettings)) => Scope.fromLocationWithSettings(location, locationSettings)
      case _                                  => Scope.fromLocationId(locationId)
    }
  }

  def queryNextInventoryCountNumberForLocationId(locationId: UUID) =
    queryNextNumber(Scope.fromLocationId(locationId), NextNumberType.InventoryCount)

  private def queryNextNumber(scope: Scope, `type`: NextNumberType) = {
    def queryUpdateOrCreateEntry(currentNumb: Option[Int]) =
      currentNumb match {
        case Some(n) =>
          queryFindByScopeTypeScopeIdAndType(scope, `type`)
            .map(t => (t.nextVal, t.updatedAt))
            .update(n + 1, UtcTime.now)
        case None => queryInsertNewNextNumber(scope, `type`, initialNumb + 1)
      }

    for {
      existingCurrentNumb <-
        queryFindByScopeTypeScopeIdAndType(scope, `type`)
          .forUpdate
          .map(_.nextVal)
          .result
          .headOption
      _ <- queryUpdateOrCreateEntry(existingCurrentNumb)
    } yield existingCurrentNumb.getOrElse(initialNumb)
  }

  def queryInsertNewNextNumber(
      scope: Scope,
      `type`: NextNumberType,
      startNumb: Int,
    ) = {
    val now = UtcTime.now
    val record = NextNumberRecord(UUID.randomUUID, scope.`type`, scope.key, `type`, startNumb, now, now)
    queryInsert(record).map(_.nextVal)
  }

  private def queryFindByScopeTypeScopeIdAndType(scope: Scope, `type`: NextNumberType) =
    table.filter(t => t.scopeType === scope.`type` && t.scopeKey === scope.key && t.`type` === `type`)

  def findByScopeAndType(scope: Scope, `type`: NextNumberType): Future[Option[Record]] = {
    val q = queryFindByScopeTypeScopeIdAndType(scope, `type`)
    run(q.result.headOption)
  }

}
