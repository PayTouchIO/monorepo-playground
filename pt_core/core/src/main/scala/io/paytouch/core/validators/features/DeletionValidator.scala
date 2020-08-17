package io.paytouch.core.validators.features

import java.util.UUID

import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

trait DeletionValidator[R <: SlickMerchantRecord] {

  def validateDeletion(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[UUID]]] =
    Future.successful(Multiple.success(ids))

}
