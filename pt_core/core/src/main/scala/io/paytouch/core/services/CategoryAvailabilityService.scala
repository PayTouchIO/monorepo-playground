package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.Availabilities
import io.paytouch.core.conversions.AvailabilityConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.AvailabilityUpdate
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class CategoryAvailabilityService(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends AvailabilityConversions {

  protected val dao = daos.categoryAvailabilityDao

  val itemType = AvailabilityItemType.Category

  def toCategoryAvailabilities(
      catalogCategoryId: UUID,
      availabilities: Option[Availabilities],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[AvailabilityUpdate]]] =
    Future.successful {
      Multiple.success(toAvailabilities(Some(catalogCategoryId), availabilities))
    }

  def findAllByCategoryIds(categoryIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Availabilities]] =
    dao.findByItemIds(categoryIds).map(groupAvailabilitiesPerItemId)
}
