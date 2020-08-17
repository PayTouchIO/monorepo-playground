package io.paytouch.core.resources.taxrates

import java.util.UUID

import io.paytouch.core.data.model.{ LocationRecord, TaxRateRecord }
import io.paytouch.core.entities.{ TaxRate, TaxRateLocationUpdate, TaxRateUpdate }
import io.paytouch.core.utils._

abstract class TaxRatesFSpec extends FSpec {

  abstract class TaxRatesResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val taxRateDao = daos.taxRateDao
    lazy val itemLocationDao = daos.taxRateLocationDao

    def assertUpdate(update: TaxRateUpdate, newTaxRateId: UUID) = {
      val record = taxRateDao.findById(newTaxRateId).await.get
      if (update.name.isDefined) update.name ==== Some(record.name)
      if (update.value.isDefined) update.value ==== Some(record.value)
      if (update.applyToPrice.isDefined) update.applyToPrice ==== Some(record.applyToPrice)

      update.locationOverrides.foreach {
        case (locationId, Some(locationUpdate)) => assertItemLocationUpdate(newTaxRateId, locationId, locationUpdate)
        case (locationId, None)                 => assertItemLocationDoesntExist(newTaxRateId, locationId)
      }
    }

    def assertItemLocationUpdate(
        itemId: UUID,
        locationId: UUID,
        update: TaxRateLocationUpdate,
      ) = {
      val record = assertItemLocationExists(itemId, locationId)
      if (update.active.isDefined) update.active ==== Some(record.active)
    }

    def assertItemLocationExists(itemId: UUID, locationId: UUID) = {
      val itemLocation = itemLocationDao.findOneByItemIdAndLocationId(itemId, locationId).await
      itemLocation must beSome
      itemLocation.get
    }

    def assertItemLocationDoesntExist(itemId: UUID, locationId: UUID) = {
      val categoryLocation = itemLocationDao.findOneByItemIdAndLocationId(itemId, locationId).await
      categoryLocation must beNone
    }

    def assertResponseById(entity: TaxRate, id: UUID) = {
      val record = taxRateDao.findById(id).await.get
      assertResponse(entity, record)
    }

    def assertResponse(
        entity: TaxRate,
        record: TaxRateRecord,
        locations: Option[Set[LocationRecord]] = None,
      ) = {
      entity.name ==== record.name
      entity.value ==== record.value
      entity.applyToPrice ==== record.applyToPrice

      if (locations.isDefined) entity.locationOverrides.map(_.keySet) ==== locations.map(_.map(_.id))
    }
  }
}
