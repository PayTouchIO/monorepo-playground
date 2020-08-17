package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ SlickRecord, SlickUpdate }
import io.paytouch.core.entities.{ MerchantContext, UpdateEntityWithRelIds, UserContext }
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._

import scala.concurrent._

trait EntityConversion[R <: SlickRecord, E] {

  implicit def ec: ExecutionContext

  def fromRecordsToEntities(records: Seq[R])(implicit user: UserContext): Seq[E] =
    records.map(fromRecordToEntity)

  def fromRecordToEntity(record: R)(implicit user: UserContext): E

  implicit def toFutureOptionEntity(f: Future[Option[R]])(implicit user: UserContext): Future[Option[E]] =
    f.map(_.map(fromRecordToEntity))
  implicit def toFutureSeqEntity(f: Future[Seq[R]])(implicit user: UserContext): Future[Seq[E]] =
    f.map(_.map(fromRecordToEntity))
  implicit def toSeqEntity(s: Seq[R])(implicit user: UserContext): Seq[E] = s.map(fromRecordToEntity)

  implicit def toFutureEntity(f: Future[R])(implicit user: UserContext): Future[E] = f.map(fromRecordToEntity)

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, R)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, E)] =
    f.map {
      case (resultType, record) =>
        (resultType, fromRecordToEntity(record))
    }

  implicit def toFutureResultTypeEntities(
      f: Future[(ResultType, Seq[R])],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Seq[E])] =
    f.map {
      case (resultType, record) =>
        (resultType, toSeqEntity(record))
    }

  implicit def toUpsertionResultEntities(
      result: (ResultType, Seq[R]),
    )(implicit
      user: UserContext,
    ): ErrorsOr[Result[Seq[E]]] = {
    val (resultType, r) = result
    UpsertionResult(resultType, r.map(fromRecordToEntity))
  }
}

trait EntityConversionNoUserContext[R <: SlickRecord, E] {

  implicit def ec: ExecutionContext

  def fromRecordsToEntities(records: Seq[R]): Seq[E] =
    records.map(fromRecordToEntity)

  def fromRecordToEntity(record: R): E

  implicit def toFutureOptionEntity(f: Future[Option[R]]): Future[Option[E]] =
    f.map(_.map(fromRecordToEntity))
  implicit def toFutureSeqEntity(f: Future[Seq[R]]): Future[Seq[E]] =
    f.map(_.map(fromRecordToEntity))
  implicit def toSeqEntity(s: Seq[R]): Seq[E] = s.map(fromRecordToEntity)

  implicit def toFutureEntity(f: Future[R]): Future[E] = f.map(fromRecordToEntity)

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, R)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, E)] =
    f.map {
      case (resultType, record) =>
        (resultType, fromRecordToEntity(record))
    }

  implicit def toFutureResultTypeEntities(f: Future[(ResultType, Seq[R])]): Future[(ResultType, Seq[E])] =
    f.map {
      case (resultType, record) =>
        (resultType, toSeqEntity(record))
    }

  implicit def toUpsertionResultEntities(result: (ResultType, Seq[R])): ErrorsOr[Result[Seq[E]]] = {
    val (resultType, r) = result
    UpsertionResult(resultType, r.map(fromRecordToEntity))
  }
}

trait EntityConversionMerchantContext[R <: SlickRecord, E] {

  implicit def ec: ExecutionContext

  def fromRecordsToEntities(records: Seq[R])(implicit merchant: MerchantContext): Seq[E] =
    records.map(fromRecordToEntity)

  def fromRecordToEntity(record: R)(implicit merchant: MerchantContext): E

  implicit def toFutureOptionEntity(f: Future[Option[R]])(implicit merchant: MerchantContext): Future[Option[E]] =
    f.map(_.map(fromRecordToEntity))
  implicit def toFutureSeqEntity(f: Future[Seq[R]])(implicit merchant: MerchantContext): Future[Seq[E]] =
    f.map(_.map(fromRecordToEntity))
  implicit def toSeqEntity(s: Seq[R])(implicit merchant: MerchantContext): Seq[E] = s.map(fromRecordToEntity)

  implicit def toFutureEntity(f: Future[R])(implicit merchant: MerchantContext): Future[E] = f.map(fromRecordToEntity)

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, R)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, E)] =
    f.map {
      case (resultType, record) =>
        (resultType, fromRecordToEntity(record)(user.toMerchantContext))
    }

  implicit def toFutureResultTypeEntities(
      f: Future[(ResultType, Seq[R])],
    )(implicit
      merchant: MerchantContext,
    ): Future[(ResultType, Seq[E])] =
    f.map {
      case (resultType, record) =>
        (resultType, toSeqEntity(record))
    }

  implicit def toUpsertionResultEntities(
      result: (ResultType, Seq[R]),
    )(implicit
      merchant: MerchantContext,
    ): ErrorsOr[Result[Seq[E]]] = {
    val (resultType, r) = result
    UpsertionResult(resultType, r.map(fromRecordToEntity))
  }
}

trait ModelConversion[T, U <: SlickUpdate[_]] {

  def fromUpsertionToUpdate(id: UUID, t: T)(implicit user: UserContext): U
}

trait ModelWithIdConversion[T <: UpdateEntityWithRelIds[_], U <: SlickUpdate[_]] {

  def fromUpdateEntitiesToModels(ts: Seq[T])(implicit user: UserContext): Seq[U] =
    ts.map(fromUpdateEntityToModel)

  def fromUpdateEntityToModel(t: T)(implicit user: UserContext): U
}
