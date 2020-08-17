package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.{ Availabilities, AvailabilitiesPerItemId }
import io.paytouch.core.conversions.AvailabilityConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.AvailabilityUpdate
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import scala.concurrent._

class DiscountAvailabilityService(implicit val ec: ExecutionContext, val daos: Daos) extends AvailabilityConversions {

  protected val dao = daos.discountAvailabilityDao
  def itemType = AvailabilityItemType.Discount

  def findAllPerDiscount(discountIds: Seq[UUID])(implicit user: UserContext): Future[AvailabilitiesPerItemId] =
    for {
      availabilities <- dao.findByItemIds(discountIds)
    } yield groupAvailabilitiesPerItemId(availabilities)

  def toDiscountAvailabilities(
      discountId: UUID,
      availabilityHours: Option[Availabilities],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[AvailabilityUpdate]]]] =
    Future.successful {
      availabilityHours match {
        case Some(availabilities) =>
          val daysPerAvailability = groupDaysPerAvailability(availabilities)
          val availabilityUpdates = daysPerAvailability.map {
            case (availability, days) =>
              toAvailabilityUpdate(discountId, days, availability)
          }.toSeq
          Multiple.successOpt(availabilityUpdates)
        case None => Multiple.empty
      }
    }
}
