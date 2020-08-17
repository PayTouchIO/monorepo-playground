package io.paytouch.ordering.services.features

import java.util.UUID

import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.AppContext
import io.paytouch.ordering.validators.features.ValidatorWithExtraFields

import scala.concurrent.Future

trait FindByIdFeature extends EnrichFeature { self =>
  type Context <: AppContext
  type Record <: SlickRecord
  type Validator <: ValidatorWithExtraFields { type Record = self.Record; type Context = self.Context }

  protected def validator: Validator

  def findById(id: UUID)(implicit context: Context): Future[Option[Entity]] =
    findById(id, defaultFilters, defaultExpansions)

  def findById(id: UUID, filters: Filters)(implicit context: Context): Future[Option[Entity]] =
    findById(id, filters, defaultExpansions)

  def findById(id: UUID, expansions: Expansions)(implicit context: Context): Future[Option[Entity]] =
    findById(id, defaultFilters, expansions)

  def findById(
      id: UUID,
      filters: Filters,
      expansions: Expansions,
    )(implicit
      context: Context,
    ): Future[Option[Entity]] =
    validator.accessOneById(id).flatMapValid(record => enrich(record, filters, expansions)).map(_.toOption)
}
