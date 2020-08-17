package io.paytouch.ordering
package services.features

import java.util.UUID

import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.AppContext

import scala.concurrent.Future

trait FindExpansionByRecordFeature extends EnrichFeature {

  protected def findEntitiesByRecord[R <: SlickRecord](
      records: Seq[R],
      finderF: Seq[UUID] => Future[Seq[Record]],
      groupByKeyF: Record => UUID,
    )(implicit
      context: AppContext,
    ): Future[Map[R, Seq[Entity]]] =
    for {
      expandingRecords <- finderF(records.map(_.id))
      recordEntities <- enrichZip(expandingRecords)
    } yield recordEntities
      .groupBy { case (record, _) => groupByKeyF(record) }
      .transform((_, v) => v.map { case (_, entity) => entity })
      .toMap
      .mapKeysToRecords(records)
}
