package io.paytouch.ordering.services

import java.util.UUID

import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.StoreRecord
import io.paytouch.ordering.entities.{ Ids, IdsUsage, UsageAnalysis }

import scala.concurrent.{ ExecutionContext, Future }

class IdService(implicit val ec: ExecutionContext, val daos: Daos) {

  private val storeDao = daos.storeDao

  def checkUsage(merchantId: UUID, idsToCheck: Ids): Future[IdsUsage] =
    checkCatalogIdsUsage(merchantId, idsToCheck.catalogIds).map(IdsUsage(_))

  private def checkCatalogIdsUsage(merchantId: UUID, catalogIds: Seq[UUID]): Future[UsageAnalysis] =
    storeDao.findAllByCatalogIds(catalogIds).map { records =>
      UsageAnalysis.extract[StoreRecord](catalogIds, records)(
        idsExtractor = _.map(_.catalogId),
        matcher = _.merchantId == merchantId,
      )
    }

}
