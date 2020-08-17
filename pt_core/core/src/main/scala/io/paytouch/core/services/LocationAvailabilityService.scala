package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.AvailabilitiesPerItemId
import io.paytouch.core.conversions.AvailabilityConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.entities.UserContext

import scala.concurrent._

class LocationAvailabilityService(implicit val ec: ExecutionContext, val daos: Daos) extends AvailabilityConversions {

  protected val dao = daos.locationAvailabilityDao
  val itemType = AvailabilityItemType.Location

  def findAllPerLocation(locationIds: Seq[UUID])(implicit user: UserContext): Future[AvailabilitiesPerItemId] =
    for {
      availabilities <- dao.findByItemIds(locationIds)
    } yield groupAvailabilitiesPerItemId(availabilities)
}
