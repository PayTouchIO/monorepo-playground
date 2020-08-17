package io.paytouch.core.resources.catalogs

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.CatalogAvailabilityDao
import io.paytouch.core.data.model.CatalogRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.utils._

abstract class CatalogsFSpec extends FSpec {
  abstract class CatalogResourceFSpecContext
      extends FSpecContext
         with DefaultFixtures
         with AvailabilitiesSupport[CatalogAvailabilityDao] {
    val catalogDao = daos.catalogDao
    val productCategoryDao = daos.productCategoryDao
    val availabilityDao = daos.catalogAvailabilityDao

    def assertResponse(
        entity: Catalog,
        record: CatalogRecord,
        productsCount: Option[Int] = None,
        categoriesCount: Option[Int] = None,
      ) = {
      entity.id ==== record.id
      entity.name ==== record.name
      entity.`type` ==== record.`type`
      entity.productsCount ==== productsCount
      entity.categoriesCount ==== categoriesCount
    }

    def assertCreation(catalogId: UUID, creation: CatalogCreation) =
      assertUpdate(catalogId, creation.asUpdate)

    def assertUpdate(catalogId: UUID, update: CatalogUpsertion) = {
      val catalog = catalogDao.findById(catalogId).await.get
      if (update.name.isDefined) update.name ==== catalog.name.some
      if (update.availabilities.isDefined) assertAvailabilityUpsertion(catalogId, update.availabilities.get)
    }
  }
}
