package io.paytouch.core.services.features

import java.util.UUID

import cats.implicits._

import scala.concurrent._

import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities._
import io.paytouch.core.validators.features.ValidatorWithExtraFields

trait FindByIdFeature extends EnrichFeature { self =>
  type Record <: SlickMerchantRecord
  type Validator <: ValidatorWithExtraFields[Record]

  def defaultFilters: Filters
  protected def validator: Validator

  def findById(
      id: UUID,
      filters: Filters = defaultFilters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Option[Entity]] =
    validator.accessOneById(id).flatMapTraverse(item => enrich(item, filters)(expansions)).map(_.toOption)
}
