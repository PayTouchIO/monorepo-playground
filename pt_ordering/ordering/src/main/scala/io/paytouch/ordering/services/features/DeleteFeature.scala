package io.paytouch.ordering.services.features

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.ordering.data.daos.features.SlickDao
import io.paytouch.ordering.entities.AppContext
import io.paytouch.ordering.utils.Implicits
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.features.DeletionValidator

trait DeleteFeature extends Implicits { self =>
  type Context <: AppContext
  type Dao <: SlickDao
  type Validator <: DeletionValidator { type Context = self.Context }

  protected def dao: Dao
  protected def validator: Validator

  def delete(id: UUID)(implicit context: Context): Future[ValidatedData[Unit]] =
    bulkDelete(Seq(id))

  def bulkDelete(ids: Seq[UUID])(implicit context: Context): Future[ValidatedData[Unit]] =
    validator
      .validateDeletion(ids)
      .flatMapValid(deleteByIds)
      .map(_.void)

  protected def deleteByIds(ids: Seq[UUID])(implicit context: Context): Future[Seq[UUID]]
}
