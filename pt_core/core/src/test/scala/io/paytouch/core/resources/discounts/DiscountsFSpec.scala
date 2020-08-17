package io.paytouch.core.resources.discounts

import java.util.UUID

import io.paytouch.core.data.daos.{ DiscountAvailabilityDao, DiscountLocationDao }
import io.paytouch.core.data.model.enums.DiscountType
import io.paytouch.core.data.model.{ DiscountLocationRecord, DiscountRecord, LocationRecord }
import io.paytouch.core.entities.{ Discount => DiscountEntity, _ }
import io.paytouch.core.utils._

abstract class DiscountsFSpec extends FSpec {

  abstract class DiscountResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with AvailabilitiesSupport[DiscountAvailabilityDao]
         with ItemLocationSupport[DiscountLocationDao, DiscountLocationRecord, ItemLocationUpdate] {

    val discountDao = daos.discountDao
    val itemLocationDao = daos.discountLocationDao
    val availabilityDao = daos.discountAvailabilityDao

    def assertResponse(
        record: DiscountRecord,
        entity: DiscountEntity,
        locations: Option[Seq[LocationRecord]] = None,
      ) = {
      record.id ==== entity.id
      record.title ==== entity.title
      record.`type` ==== entity.`type`
      if (record.`type` == DiscountType.Percentage) entity.currency ==== None
      else entity.currency ==== Some(currency)
      record.amount ==== entity.amount
      record.requireManagerApproval ==== entity.requireManagerApproval

      entity.locationOverrides.map(_.keySet) ==== locations.map(_.map(_.id).toSet)
    }

    def assertUpdate(discountId: UUID, update: DiscountUpdate) = {
      val discountRecord = discountDao.findById(discountId).await.get

      if (update.title.isDefined) update.title ==== Some(discountRecord.title)
      if (update.`type`.isDefined) update.`type` ==== Some(discountRecord.`type`)
      if (update.amount.isDefined) update.amount ==== Some(discountRecord.amount)
      if (update.requireManagerApproval.isDefined)
        update.requireManagerApproval ==== Some(discountRecord.requireManagerApproval)

      update.locationOverrides.foreach {
        case (locationId, Some(locationUpdate)) => assertItemLocationUpdate(discountId, locationId, locationUpdate)
        case (locationId, None)                 => assertItemLocationDoesntExist(discountId, locationId)
      }

      update.availabilityHours.map(assertAvailabilityUpsertion(discountId, _))
    }

    def assertItemLocationUpdate(
        itemId: UUID,
        locationId: UUID,
        update: ItemLocationUpdate,
      ) = {
      val record = assertItemLocationExists(itemId, locationId)
      if (update.active.isDefined) update.active ==== Some(record.active)
    }
  }
}
