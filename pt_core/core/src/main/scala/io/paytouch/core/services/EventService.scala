package io.paytouch.core.services

import io.paytouch.core.conversions.EventConversions
import io.paytouch.core.data.daos.{ Daos, EventDao }
import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.entities.{ UserContext, Event => EventEntity }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.EventFilters
import io.paytouch.core.services.features.FindAllFeature

import scala.concurrent._

class EventService(implicit val ec: ExecutionContext, val daos: Daos) extends EventConversions with FindAllFeature {

  type Dao = EventDao
  type Entity = EventEntity
  type Expansions = NoExpansions
  type Filters = EventFilters
  type Record = EventRecord

  protected val dao = daos.eventDao

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    Future.successful(toSeqEntity(records))
}
