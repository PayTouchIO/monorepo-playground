package io.paytouch.ordering.services.features

import io.paytouch.ordering.FindResult
import io.paytouch.ordering.data.daos.features.SlickFindAllDao
import io.paytouch.ordering.entities.{ AppContext, Pagination }

import scala.concurrent.Future

trait FindAllFeature extends EnrichFeature { self =>
  type Context <: AppContext
  type Dao <: SlickFindAllDao { type Record = self.Record; type Filters = self.Filters }

  protected def dao: Dao

  def findAll()(implicit context: Context, pagination: Pagination): Future[FindResult[Entity]] =
    findAll(defaultFilters, defaultExpansions)

  def findAll(filters: Filters)(implicit context: Context, pagination: Pagination): Future[FindResult[Entity]] =
    findAll(filters, defaultExpansions)

  def findAll(expansions: Expansions)(implicit context: Context, pagination: Pagination): Future[FindResult[Entity]] =
    findAll(defaultFilters, expansions)

  def findAll(
      filters: Filters,
      expansions: Expansions,
    )(implicit
      context: Context,
      pagination: Pagination,
    ): Future[FindResult[Entity]] = {
    val contextIds = context.contextIds
    val itemsR = dao.findAllWithFilters(contextIds, filters)(pagination.offset, pagination.limit)
    val countR = dao.countAllWithFilters(contextIds, filters)
    for {
      items <- itemsR
      enrichedData <- enrich(items, filters, expansions)
      count <- countR
    } yield (enrichedData, count)
  }
}
