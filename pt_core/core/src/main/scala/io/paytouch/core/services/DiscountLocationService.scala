package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.DiscountLocationConversions
import io.paytouch.core.data.daos.{ Daos, DiscountLocationDao }
import io.paytouch.core.data.model.{ DiscountLocationRecord, DiscountLocationUpdate => DiscountLocationUpdateModel }
import io.paytouch.core.entities.{ UserContext, ItemLocationUpdate => ItemLocationUpdateEntity }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.DiscountValidator

import scala.concurrent._

class DiscountLocationService(implicit val ec: ExecutionContext, val daos: Daos)
    extends ItemLocationService
       with DiscountLocationConversions {

  type Dao = DiscountLocationDao
  type Record = DiscountLocationRecord

  protected val dao = daos.discountLocationDao

  val discountValidator = new DiscountValidator

  def accessItemById(id: UUID)(implicit user: UserContext) = discountValidator.accessOneById(id)

  def convertToDiscountLocationUpdates(
      itemId: UUID,
      locationOverrides: Map[UUID, Option[ItemLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[DiscountLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq
    for {
      locations <- locationValidator.validateByIds(locationIds)
      itemLocations <- dao.findByItemIdsAndLocationIds(Seq(itemId), locationIds).map(Multiple.success)
    } yield Multiple.combine(locations, itemLocations) {
      case (_, itemLocs) =>
        locationOverrides.map {
          case (locationId, itemLocationUpdate) =>
            val discountLocationUpdate = itemLocationUpdate.map(itemLocUpd =>
              itemLocs
                .find(_.locationId == locationId)
                .map(toDiscountLocationUpdate)
                .getOrElse(toDiscountLocationUpdate(itemId, locationId))
                .copy(active = itemLocUpd.active),
            )
            (locationId, discountLocationUpdate)
        }
    }
  }

  def findByDiscountIds(
      discountIds: Seq[UUID],
      locationId: Option[UUID] = None,
    )(implicit
      user: UserContext,
    ): Future[Seq[DiscountLocationRecord]] =
    dao.findByItemIdsAndLocationIds(discountIds, user.accessibleLocations(locationId))
}
