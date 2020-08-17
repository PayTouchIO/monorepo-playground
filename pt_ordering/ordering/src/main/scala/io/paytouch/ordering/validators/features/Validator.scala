package io.paytouch.ordering.validators.features

import java.util.UUID

import io.paytouch.ordering.errors.{ Error => Err }

import scala.concurrent.Future

trait Validator extends ValidatorWithExtraFields {

  type Extra = Record

  val validationErrorF: Seq[UUID] => Err
  val accessErrorF: Seq[UUID] => Err

  protected def validityCheckWithExtraRecords(
      record: Record,
      extraRecords: Seq[Record],
    )(implicit
      context: Context,
    ): Boolean =
    validityCheck(record)

  protected def extraRecordsFinder(ids: Seq[UUID])(implicit context: Context): Future[Seq[Record]] =
    Future.successful(Seq.empty)
}
