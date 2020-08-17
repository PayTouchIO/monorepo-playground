package io.paytouch.core.validators.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors

trait Validator[Record <: SlickMerchantRecord] extends ValidatorWithExtraFields[Record] {
  type Extra = Record

  val validationErrorF: Seq[UUID] => errors.Error
  val accessErrorF: Seq[UUID] => errors.Error

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Record]]

  protected def validityCheck(record: Record)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId

  protected def validityCheckWithExtraRecords(
      record: Record,
      extraRecords: Seq[Record],
    )(implicit
      user: UserContext,
    ): Boolean =
    validityCheck(record)

  protected def extraRecordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Record]] =
    Future.successful(Seq.empty)
}
