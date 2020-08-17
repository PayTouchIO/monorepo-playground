package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.model.{ SlickItemLocationRecord, SlickToggleableRecord }
import io.paytouch.core.entities.{ ItemLocation, UpdateActiveLocation, UserContext }
import io.paytouch.core.LocationOverridesPer
import io.paytouch.core.utils.{ Implicits, Multiple }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.LocationValidator

trait ItemLocationService extends Implicits { self =>
  type Record <: SlickItemLocationRecord with SlickToggleableRecord
  type Dao <: SlickItemLocationToggleableDao { type Record = self.Record }

  protected def dao: Dao

  val locationValidator = new LocationValidator

  def accessItemById(itemId: UUID)(implicit user: UserContext): Future[ErrorsOr[_]]
  def fromItemLocationsToEntities(itemLocations: Seq[Record]): Seq[ItemLocation] =
    itemLocations.map(fromItemLocationToEntity)

  def fromItemLocationToEntity(itemLocationRecord: Record): ItemLocation =
    ItemLocation(active = itemLocationRecord.active)

  def updateActiveLocationsByItemId(
      id: UUID,
      updateActiveLocations: Seq[UpdateActiveLocation],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    (for {
      item <- accessItemById(id)
      locations <- locationValidator.accessByIds(updateActiveLocations.map(_.locationId))
    } yield (item, locations).tupled).flatMapTraverse { _ =>
      dao.updateActiveByRelIds(id, updateActiveLocations).void
    }

  def findAllByItemIds(itemIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[ItemLocation]]] =
    dao
      .findByItemIdsAndLocationIds(itemIds, user.locationIds)
      .map(groupLocationsPerItem)

  private def groupLocationsPerItem(itemLocations: Seq[Record]): Map[UUID, Seq[ItemLocation]] =
    itemLocations
      .groupBy(_.itemId)
      .transform((_, v) => fromItemLocationsToEntities(v))

  def findAllByItemIdsAsMap(
      itemIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[LocationOverridesPer[UUID, ItemLocation]] =
    dao.findByItemIdsAndLocationIds(itemIds, user.accessibleLocations(locationIds)).map(groupLocationsPerItemMap)

  private def groupLocationsPerItemMap(itemLocations: Seq[Record]): LocationOverridesPer[UUID, ItemLocation] =
    itemLocations.groupBy(_.itemId).map {
      case (itemId, itemsLocs) =>
        itemId -> fromItemLocationsToEntitiesMap(itemsLocs)
    }

  private def fromItemLocationsToEntitiesMap(itemLocations: Seq[Record]): Map[UUID, ItemLocation] =
    itemLocations.map(itemLoc => itemLoc.locationId -> fromItemLocationToEntity(itemLoc)).toMap
}
