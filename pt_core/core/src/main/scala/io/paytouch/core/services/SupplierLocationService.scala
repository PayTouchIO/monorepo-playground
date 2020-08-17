package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.SupplierLocationConversions
import io.paytouch.core.data.daos.{ Daos, SupplierLocationDao }
import io.paytouch.core.data.model.{ SupplierLocationRecord, SupplierLocationUpdate => SupplierLocationUpdateModel }
import io.paytouch.core.entities.{ UserContext, ItemLocationUpdate => ItemLocationUpdateEntity }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.SupplierValidator

import scala.concurrent._

class SupplierLocationService(implicit val ec: ExecutionContext, val daos: Daos)
    extends ItemLocationService
       with SupplierLocationConversions {

  type Dao = SupplierLocationDao
  type Record = SupplierLocationRecord

  protected val dao = daos.supplierLocationDao

  val supplierValidator = new SupplierValidator

  def accessItemById(id: UUID)(implicit user: UserContext) = supplierValidator.accessOneById(id)

  def convertToSupplierLocationUpdates(
      itemId: UUID,
      locationOverrides: Map[UUID, Option[ItemLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[SupplierLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq
    for {
      locations <- locationValidator.validateByIds(locationIds)
      itemLocations <- dao.findByItemIdsAndLocationIds(Seq(itemId), locationIds).map(Multiple.success)
    } yield Multiple.combine(locations, itemLocations) {
      case (_, itemLocs) =>
        locationOverrides.map {
          case (locationId, itemLocationUpdate) =>
            val supplierLocationUpdate = itemLocationUpdate.map(itemLocUpd =>
              itemLocs
                .find(_.locationId == locationId)
                .map(toSupplierLocationUpdate)
                .getOrElse(toSupplierLocationUpdate(itemId, locationId))
                .copy(active = itemLocUpd.active),
            )
            (locationId, supplierLocationUpdate)
        }
    }
  }

  def findBySupplierIds(
      supplierIds: Seq[UUID],
      locationId: Option[UUID] = None,
    )(implicit
      user: UserContext,
    ): Future[Seq[Record]] =
    dao.findByItemIdsAndLocationIds(supplierIds, user.accessibleLocations(locationId))
}
