package io.paytouch.core.services.features

import io.paytouch.core.data.daos.features.SlickFindAllDao
import io.paytouch.core.entities._
import io.paytouch.core.utils.FindResult._

import scala.concurrent._

trait FindAllFeature extends EnrichFeature { self =>

  type Dao <: SlickFindAllDao { type Record = self.Record; type Filters = self.Filters }

  protected def dao: Dao

  def findAll(
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): Future[FindResult[Entity]] = {
    val merchantId = user.merchantId
    val itemsResp = dao.findAllWithFilters(merchantId, filters)(pagination.offset, pagination.limit)
    val countResp = dao.countAllWithFilters(merchantId, filters)
    for {
      items <- itemsResp
      enrichedData <- enrich(items, filters)(expansions)
      count <- countResp
    } yield (enrichedData, count)
  }
}
