package io.paytouch.core.services.features

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.entities._
import io.paytouch.core.services.ItemLocationService
import io.paytouch.core.utils.Multiple._

trait UpdateActiveLocationsFeature {
  def itemLocationService: ItemLocationService

  def updateActiveLocations(
      id: UUID,
      updateActiveLocations: Seq[UpdateActiveLocation],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    itemLocationService.updateActiveLocationsByItemId(id, updateActiveLocations)
}
