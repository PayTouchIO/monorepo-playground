package io.paytouch.ordering.validators.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.ordering.entities.AppContext
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

trait DeletionValidator {
  type Context <: AppContext

  def validateDeletion(ids: Seq[UUID])(implicit context: Context): Future[ValidatedData[Seq[UUID]]] =
    Future.successful(ValidatedData.success(ids))
}
