package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.ModifierSetLocationConversions
import io.paytouch.core.data.daos.{ Daos, ModifierSetLocationDao }
import io.paytouch.core.data.model.{
  ModifierSetLocationRecord,
  ModifierSetLocationUpdate => ModifierSetLocationUpdateModel,
}
import io.paytouch.core.entities.{ ItemLocationUpdate => ItemLocationUpdateEntity, _ }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.ModifierSetValidator

import scala.concurrent._

class ModifierSetLocationService(implicit val ec: ExecutionContext, val daos: Daos)
    extends ItemLocationService
       with ModifierSetLocationConversions {

  type Dao = ModifierSetLocationDao
  type Record = ModifierSetLocationRecord

  protected val dao = daos.modifierSetLocationDao

  val modifierSetValidator = new ModifierSetValidator

  def accessItemById(id: UUID)(implicit user: UserContext) = modifierSetValidator.accessOneById(id)

  def convertToModifierSetLocationUpdates(
      itemId: UUID,
      locationOverrides: Map[UUID, Option[ItemLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[ModifierSetLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq

    for {
      locations <- locationValidator.validateByIds(locationIds)
      itemLocations <- dao.findByItemIdsAndLocationIds(Seq(itemId), locationIds).map(Multiple.success)
    } yield Multiple.combine(locations, itemLocations) { (_, itemLocs) =>
      locationOverrides.map {
        case (locationId, itemLocationUpdate) =>
          locationId -> itemLocationUpdate.map { itemLocUpd =>
            itemLocs
              .find(_.locationId == locationId)
              .map(toModifierSetLocationUpdate)
              .getOrElse(toModifierSetLocationUpdate(itemId, locationId))
              .copy(active = itemLocUpd.active)
          }
      }
    }
  }
}
