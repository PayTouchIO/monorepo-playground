package io.paytouch.core.utils

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickItemLocationDao
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.entities.ItemLocationUpdateEntity
import io.paytouch.utils.FutureHelpers
import org.specs2.matcher.{ Matchers, MustThrownExpectations }

trait ItemLocationSupport[
    D <: SlickItemLocationDao { type Record = R },
    R <: SlickRecord,
    LocUpdate <: ItemLocationUpdateEntity,
  ] extends MustThrownExpectations
       with Matchers
       with FutureHelpers {

  def itemLocationDao: D

  def assertItemLocationUpdate(
      itemId: UUID,
      locationId: UUID,
      update: LocUpdate,
    ): Unit

  def assertLocationOverridesUpdate(locationOverrides: Map[UUID, Option[LocUpdate]], itemId: UUID) =
    locationOverrides.foreach {
      case (locationId, Some(locationUpdate)) => assertItemLocationUpdate(itemId, locationId, locationUpdate)
      case (locationId, None)                 => assertItemLocationDoesntExist(itemId, locationId)
    }

  def assertItemLocationExists(itemId: UUID, locationId: UUID): R = {
    val itemLocation = itemLocationDao.findOneByItemIdAndLocationId(itemId, locationId).await
    itemLocation must beSome
    itemLocation.get
  }
  def assertItemLocationDoesntExist(itemId: UUID, locationId: UUID) = {
    val itemLocation = itemLocationDao.findOneByItemIdAndLocationId(itemId, locationId).await
    itemLocation must beNone
  }
}
