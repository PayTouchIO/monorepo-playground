package io.paytouch.ordering.validators.features

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.AppContext
import io.paytouch.ordering.errors.{ Error => Err }
import io.paytouch.ordering.utils.Implicits
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData
import io.paytouch.ordering.utils.validation.{ ValidatedData, ValidatedOptData }

import scala.concurrent.Future

trait ValidatorWithExtraFields extends Implicits {

  type Context <: AppContext
  type Record <: SlickRecord
  type Extra <: SlickRecord

  val validationErrorF: Seq[UUID] => Err
  val accessErrorF: Seq[UUID] => Err

  protected def recordsFinder(ids: Seq[UUID])(implicit context: Context): Future[Seq[Record]]

  protected def validityCheck(record: Record)(implicit context: Context): Boolean

  protected def validityCheckWithExtraRecords(
      record: Record,
      extraRecords: Seq[Extra],
    )(implicit
      context: Context,
    ): Boolean

  protected def extraRecordsFinder(ids: Seq[UUID])(implicit context: Context): Future[Seq[Extra]]

  protected def columnMapper(record: Record) = record.id

  def accessByIds(ids: Seq[UUID])(implicit context: Context): Future[ValidatedData[Seq[Record]]] = {
    val recordsR = recordsFinder(ids)
    val extraRecordsR = extraRecordsFinder(ids)
    for {
      records <- recordsR
      extraRecords <- extraRecordsR
    } yield {
      val areAccessible = ids.distinct.size == records.distinct.size && records.forall(
        validityCheckWithExtraRecords(_, extraRecords),
      )
      if (areAccessible) ValidatedData.success(records)
      else {
        val nonAccessibleIds = ids diff records
          .filter(validityCheckWithExtraRecords(_, extraRecords))
          .map(columnMapper)
        ValidatedData.failure(accessErrorF(nonAccessibleIds))
      }
    }
  }

  def accessOneById(id: UUID)(implicit context: Context): Future[ValidatedData[Record]] =
    accessByIds(Seq(id)).map {
      case Valid(records) if records.nonEmpty => ValidatedData.success(records.head)
      case Valid(_)                           => ValidatedData.failure(accessErrorF(Seq(id)))
      case i @ Invalid(_)                     => i
    }

  def accessOneByOptId(optId: Option[UUID])(implicit context: Context): Future[ValidatedOptData[Record]] =
    optId match {
      case Some(id) => accessOneById(id).mapValid(Some(_))
      case None     => Future.successful(ValidatedOptData.empty)
    }

  def availableByIds(ids: Seq[UUID])(implicit context: Context): Future[ValidatedData[Unit]] =
    recordsFinder(ids).map { records =>
      if (records.isEmpty) ValidatedData.success(())
      else {
        val nonAvailableIds = records.map(columnMapper) diff ids
        ValidatedData.failure(validationErrorF(nonAvailableIds))
      }
    }

  def availableOneById(id: UUID)(implicit context: Context): Future[ValidatedData[Unit]] =
    availableByIds(Seq(id))

  def validateByIds(ids: Seq[UUID])(implicit context: Context): Future[ValidatedData[Seq[Record]]] = {
    val recordsR = recordsFinder(ids)
    val extraRecordsR = extraRecordsFinder(ids)
    for {
      records <- recordsR
      extraRecords <- extraRecordsR
    } yield validateRecordsWithExtraRecords(records, extraRecords)
  }

  private def validateRecordsWithExtraRecords(
      records: Seq[Record],
      extraRecords: Seq[Extra],
    )(implicit
      context: Context,
    ) = {
    val areValid = records.forall(validityCheckWithExtraRecords(_, extraRecords))
    if (!areValid) {
      val invalidIds = records.filterNot(validityCheckWithExtraRecords(_, extraRecords)).map(columnMapper)
      ValidatedData.failure(validationErrorF(invalidIds))
    }
    else ValidatedData.success(records)
  }

  def validateOneById(id: UUID)(implicit context: Context): Future[ValidatedOptData[Record]] =
    validateByIds(Seq(id)).mapValid(_.headOption)

  def filterValidByIds(ids: Seq[UUID])(implicit context: Context): Future[Seq[Record]] = {
    val recordsR = recordsFinder(ids)
    val extraRecordsR = extraRecordsFinder(ids)
    for {
      records <- recordsR
      extraRecords <- extraRecordsR
    } yield records.filter(validityCheckWithExtraRecords(_, extraRecords))
  }

  def filterNonAlreadyTakenIds(ids: Seq[UUID])(implicit context: Context): Future[Seq[UUID]] =
    recordsFinder(ids).map { records =>
      val takenIds = records.collect { case r if validityCheck(r) => r.id }
      ids.distinct diff takenIds
    }
}
