package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.Availabilities
import io.paytouch.core.conversions.AvailabilityConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.AvailabilityUpdate
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

class CatalogAvailabilityService(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends AvailabilityConversions {
  protected val dao = daos.catalogAvailabilityDao

  val itemType = AvailabilityItemType.Catalog

  def toCatalogAvailabilities(
      catalogId: UUID,
      availabilities: Option[Availabilities],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[AvailabilityUpdate]]] =
    Future.successful {
      Multiple.success(toAvailabilities(Some(catalogId), availabilities))
    }

  def findAllByCatalogIds(catalogIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Availabilities]] =
    dao.findByItemIds(catalogIds).map(groupAvailabilitiesPerItemId)
}
