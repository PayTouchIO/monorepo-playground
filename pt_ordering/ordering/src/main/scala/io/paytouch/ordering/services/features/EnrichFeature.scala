package io.paytouch.ordering.services.features

import scala.concurrent.Future

import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.AppContext
import io.paytouch.ordering.expansions.BaseExpansions
import io.paytouch.ordering.filters.BaseFilters
import io.paytouch.ordering.utils.Implicits

trait EnrichFeature extends Implicits {
  type Record <: SlickRecord
  type Entity
  type Expansions <: BaseExpansions
  type Filters <: BaseFilters

  protected def defaultFilters: Filters
  protected def defaultExpansions: Expansions
  protected def expanders: Seq[Expander[this.type]]

  def enrich(record: Record)(implicit context: AppContext): Future[Entity] =
    enrich(record, defaultFilters, defaultExpansions)

  def enrich(record: Record, filters: Filters)(implicit context: AppContext): Future[Entity] =
    enrich(record, filters, defaultExpansions)

  def enrich(record: Record, expansions: Expansions)(implicit context: AppContext): Future[Entity] =
    enrich(record, defaultFilters, expansions)

  def enrich(
      record: Record,
      filters: Filters,
      expansions: Expansions,
    )(implicit
      context: AppContext,
    ): Future[Entity] =
    enrich(Seq(record), filters, expansions).map(_.head)

  def enrich(records: Seq[Record])(implicit context: AppContext): Future[Seq[Entity]] =
    enrich(records, defaultFilters, defaultExpansions)

  def enrich(records: Seq[Record], filters: Filters)(implicit context: AppContext): Future[Seq[Entity]] =
    enrich(records, filters, defaultExpansions)

  def enrich(records: Seq[Record], expansions: Expansions)(implicit context: AppContext): Future[Seq[Entity]] =
    enrich(records, defaultFilters, expansions)

  def enrich(
      records: Seq[Record],
      filters: Filters,
      expansions: Expansions,
    )(implicit
      context: AppContext,
    ): Future[Seq[Entity]] =
    enrichZip(records, filters, expansions).map {
      _.map { case (_, entity) => entity }.toSeq
    }

  protected def enrichZip(records: Seq[Record])(implicit context: AppContext): Future[Seq[(Record, Entity)]] =
    enrichZip(records, defaultFilters, defaultExpansions)

  protected def enrichZip(
      records: Seq[Record],
      filters: Filters,
      expansions: Expansions,
    )(implicit
      context: AppContext,
    ): Future[Seq[(Record, Entity)]] = {
    val retrievedDataR =
      Future.sequence {
        expanders.map { expander =>
          val rs = expander.toRS(records, filters, expansions)

          expander.retriever(rs).map(data => expander -> data)
        }
      }

    retrievedDataR.map { retrievedData =>
      records.map { record =>
        val baseEntity = fromRecordToEntity(record)

        val enrichedEntity =
          retrievedData.foldLeft(baseEntity) { (accEntity, expanderWithData) =>
            val expander = expanderWithData._1
            val data: expander.D1 = expanderWithData._2.asInstanceOf[expander.D1]
            val d2 = expander.dataExtractor(record, data)

            expander.copier(accEntity, d2)
          }

        record -> enrichedEntity
      }
    }
  }

  protected def fromRecordToEntity(record: Record)(implicit context: AppContext): Entity
}
