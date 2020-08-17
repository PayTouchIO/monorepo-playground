package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.errors.NonAccessibleIds
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.Multiple

trait RecoveryValidatorUtils {

  protected def recoverIdInSeq(
      optId: Option[UUID],
      validIs: Seq[UUID],
      errorMsg: Seq[UUID] => NonAccessibleIds,
    ): ErrorsOr[Option[UUID]] =
    optId match {
      case Some(id) if !validIs.contains(id) => Multiple.failure(errorMsg(Seq(id)))
      case maybeId                           => Multiple.success(maybeId)
    }

  protected def validateIdInSeq(
      optId: Option[UUID],
      validIs: Seq[UUID],
      errorMsg: Seq[UUID] => NonAccessibleIds,
    ): ErrorsOr[UUID] =
    optId match {
      case Some(id) if !validIs.contains(id) => Multiple.failure(errorMsg(Seq(id)))
      case Some(id)                          => Multiple.success(id)
      case None                              => Multiple.failure(errorMsg(Seq.empty))
    }
}
