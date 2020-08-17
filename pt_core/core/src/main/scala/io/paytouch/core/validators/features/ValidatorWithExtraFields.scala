package io.paytouch.core.validators.features

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.data.model.{ SlickMerchantRecord, SlickRecord }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr

trait ValidatorWithExtraFields[Record <: SlickMerchantRecord] extends Implicits {
  type Id = Record#Id
  type Extra <: SlickRecord

  val validationErrorF: Seq[Id] => errors.Error
  val accessErrorF: Seq[Id] => errors.Error

  protected def recordsFinder(ids: Seq[Id])(implicit user: UserContext): Future[Seq[Record]]

  protected def validityCheckWithExtraRecords(
      record: Record,
      extraRecords: Seq[Extra],
    )(implicit
      user: UserContext,
    ): Boolean

  protected def extraRecordsFinder(ids: Seq[Id])(implicit user: UserContext): Future[Seq[Extra]]

  trait ColumnMapper {
    def apply(record: Record): record.Id = record.id
  }
  protected val columnMapper = new ColumnMapper {}

  def accessByIds(ids: Seq[Id])(implicit user: UserContext): Future[ErrorsOr[Seq[Record]]] = {
    val recordsR = recordsFinder(ids)
    val extraRecordsR = extraRecordsFinder(ids)

    for {
      records <- recordsR
      extraRecords <- extraRecordsR
    } yield {
      val areAccessible: Boolean =
        ids.distinct.size == records.distinct.size &&
          records.forall(validityCheckWithExtraRecords(_, extraRecords))

      if (areAccessible)
        Multiple.success(records)
      else {
        val nonAccessibleIds =
          ids.diff(
            records
              .filter(validityCheckWithExtraRecords(_, extraRecords))
              .map(columnMapper(_)),
          )

        Multiple.failure(accessErrorF(nonAccessibleIds))
      }
    }
  }

  def accessOneById(id: Id)(implicit user: UserContext): Future[ErrorsOr[Record]] =
    accessByIds(Seq(id)).map {
      case Validated.Valid(records) if records.nonEmpty => Multiple.success(records.head)
      case Validated.Valid(_)                           => Multiple.failure(accessErrorF(Seq(id)))
      case i @ Validated.Invalid(_)                     => i
    }

  def accessOneByOptId(optId: Option[Id])(implicit user: UserContext): Future[ErrorsOr[Option[Record]]] =
    optId match {
      case Some(id) => accessOneById(id).mapNested(Some(_))
      case None     => Future.successful(Multiple.empty)
    }

  def availableByIds(ids: Seq[Id])(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    recordsFinder(ids).map { records =>
      if (records.isEmpty)
        Multiple.success(())
      else {
        val nonAvailableIds = records.map(columnMapper(_)).diff(ids)

        Multiple.failure(validationErrorF(nonAvailableIds))
      }
    }

  def availableOneById(id: Id)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    availableByIds(Seq(id))

  def validateByIds(ids: Seq[Id])(implicit user: UserContext): Future[ErrorsOr[Seq[Record]]] = {
    val recordsR = recordsFinder(ids)
    val extraRecordsR = extraRecordsFinder(ids)

    for {
      records <- recordsR
      extraRecords <- extraRecordsR
    } yield {
      val areValid: Boolean =
        records.forall(validityCheckWithExtraRecords(_, extraRecords))

      if (areValid)
        Multiple.success(records)
      else {
        val invalidIds =
          records
            .filterNot(validityCheckWithExtraRecords(_, extraRecords))
            .map(columnMapper(_))

        Multiple.failure(validationErrorF(invalidIds))
      }
    }
  }

  def validateOneById(id: Id)(implicit user: UserContext): Future[ErrorsOr[Option[Record]]] =
    validateByIds(Seq(id)).mapNested(_.headOption)

  def filterValidByIds(ids: Seq[Id])(implicit user: UserContext): Future[Seq[Record]] = {
    val recordsR = recordsFinder(ids)
    val extraRecordsR = extraRecordsFinder(ids)

    for {
      records <- recordsR
      extraRecords <- extraRecordsR
    } yield records.filter(validityCheckWithExtraRecords(_, extraRecords))
  }

  def filterNonAlreadyTakenIds(ids: Seq[Id])(implicit user: UserContext): Future[Seq[Id]] =
    filterNonAlreadyTakenIds(ids, _.merchantId == user.merchantId)

  def filterNonAlreadyTakenIds(ids: Seq[Id], p: Record => Boolean)(implicit user: UserContext): Future[Seq[Id]] =
    recordsFinder(ids).map { records =>
      val takenIds = records.filterNot(p).map(_.id)

      ids.distinct diff takenIds
    }
}
