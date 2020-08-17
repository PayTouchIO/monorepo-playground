package io.paytouch.core.resources.suppliers

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class SuppliersUpdateActiveFSpec extends GenericItemUpdateActiveFSpec[SupplierRecord, SupplierLocationRecord] {

  lazy val supplierLocationDao = daos.supplierLocationDao
  lazy val supplierDao = daos.supplierDao

  def finder(id: UUID) = supplierLocationDao.findById(id)

  def itemFinder(id: UUID) = supplierDao.findById(id)

  def namespace = "suppliers"

  def singular = "supplier"

  def itemFactory(merchant: MerchantRecord) = Factory.supplier(merchant).create

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: SupplierRecord,
      location: LocationRecord,
      active: Option[Boolean],
    ) =
    Factory.supplierLocation(item, location, active = active).create
}
