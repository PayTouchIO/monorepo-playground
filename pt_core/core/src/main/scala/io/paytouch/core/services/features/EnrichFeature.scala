package io.paytouch.core.services.features

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.entities._
import io.paytouch.core.expansions.BaseExpansions
import io.paytouch.core.filters.BaseFilters
import io.paytouch.core.RichMap
import io.paytouch.core.utils.Implicits

trait EnrichFeature extends Implicits {
  type Entity <: ExposedEntity
  type Expansions <: BaseExpansions
  type Filters <: BaseFilters
  type Record <: SlickRecord

  type DataByRecord[T] = Option[Map[Record, T]]
  type DataSeqByRecord[T] = DataByRecord[Seq[T]]

  def enrich(record: Record, filters: Filters)(expansions: Expansions)(implicit user: UserContext): Future[Entity] =
    enrich(Seq(record), filters)(expansions).map(_.head)

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]]

  def enrich(
      maybeRecord: Option[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    maybeRecord match {
      case Some(record) => enrich(record, filters)(expansions).map(_.some)
      case None         => Future.successful(None)
    }

  final protected def getRelatedField[T](
      findExpansionRecords: Seq[UUID] => Future[Seq[T]],
      identifierExtractor: T => UUID,
      fieldExtractor: Record => UUID,
      records: Seq[Record],
    ): Future[Map[Record, T]] =
    findExpansionRecords(records.map(fieldExtractor))
      .map { expansionRecords =>
        records.flatMap { record =>
          expansionRecords
            .find(identifierExtractor(_) == fieldExtractor(record))
            .map(record -> _)
        }.toMap
      }

  final protected def getExpandedField[T](
      findRecords: Seq[UUID] => Future[Seq[T]],
      identifierExtractor: T => UUID,
      fieldExtractor: Record => UUID,
      records: Seq[Record],
      flag: Boolean,
    ): Future[DataByRecord[T]] =
    if (flag)
      getRelatedField(findRecords, identifierExtractor, fieldExtractor, records).map(_.some)
    else
      Future.successful(None)

  final protected def getExpandedOptionalField[T](
      findRecords: Seq[UUID] => Future[Seq[T]],
      identifierExtractor: T => UUID,
      fieldExtractor: Record => Option[UUID],
      records: Seq[Record],
      flag: Boolean,
    ): Future[DataByRecord[T]] =
    if (flag)
      findRecords(records.flatMap(r => fieldExtractor(r)))
        .map { expandedFieldRecord =>
          val fieldRecordPerPurchaseOrder =
            records.flatMap { record =>
              expandedFieldRecord
                .find(fieldRecord => fieldExtractor(record).contains(identifierExtractor(fieldRecord)))
                .map(record -> _)
            }

          fieldRecordPerPurchaseOrder.toMap.some
        }
    else
      Future.successful(None)

  final protected def getExpandedMappedField[T](
      findRecords: Seq[UUID] => Future[Map[UUID, T]],
      fieldExtractor: Record => UUID,
      records: Seq[Record],
      flag: Boolean,
    ): Future[DataByRecord[T]] =
    if (flag)
      findRecords(records.map(fieldExtractor))
        .map(_.mapKeysToRecords(records).some)
    else
      Future.successful(None)
}
