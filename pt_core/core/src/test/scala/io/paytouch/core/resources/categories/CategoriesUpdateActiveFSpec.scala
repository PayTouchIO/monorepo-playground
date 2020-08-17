package io.paytouch.core.resources.categories

import java.util.UUID

import io.paytouch.core.data.model.{ CategoryLocationRecord, CategoryRecord, LocationRecord, MerchantRecord }
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CategoriesUpdateActiveFSpec extends GenericItemUpdateActiveFSpec[CategoryRecord, CategoryLocationRecord] {

  lazy val catalogDao = daos.catalogDao
  lazy val categoryLocationDao = daos.categoryLocationDao
  lazy val categoryDao = daos.categoryDao

  def finder(id: UUID) = categoryLocationDao.findById(id)

  def itemFinder(id: UUID) = categoryDao.findById(id)

  def namespace = "categories"

  def singular = "category"

  def itemFactory(merchant: MerchantRecord) = {
    val defaultMenuCatalog = catalogDao.findByMerchantIdAndType(merchant.id, CatalogType.DefaultMenu).await match {
      case Some(catalog) => catalog
      case None          => Factory.defaultMenuCatalog(merchant).create
    }
    Factory.systemCategory(defaultMenuCatalog).create
  }

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: CategoryRecord,
      location: LocationRecord,
      active: Option[Boolean],
    ) =
    Factory.categoryLocation(item, location, active = active).create
}
