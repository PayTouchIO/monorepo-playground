package io.paytouch.core.resources.suppliers

import java.util.UUID

import io.paytouch.core.data.daos.SupplierLocationDao
import io.paytouch.core.data.model.{ SupplierLocationRecord, SupplierRecord }
import io.paytouch.core.entities.{
  ItemLocation,
  ItemLocationUpdate,
  MonetaryAmount,
  SupplierCreation,
  SupplierUpdate,
  Supplier => SupplierEntity,
}
import io.paytouch.core.utils._

abstract class SuppliersFSpec extends FSpec {

  abstract class SupplierResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with ItemLocationSupport[SupplierLocationDao, SupplierLocationRecord, ItemLocationUpdate] {
    val supplierDao = daos.supplierDao
    val itemLocationDao = daos.supplierLocationDao
    val supplierProductDao = daos.supplierProductDao

    def assertCreation(creation: SupplierCreation, supplierId: UUID) =
      assertUpdate(creation.asUpdate, supplierId)

    def assertUpdate(update: SupplierUpdate, supplierId: UUID) = {
      val supplier = supplierDao.findById(supplierId).await.get
      if (update.name.isDefined) update.name ==== Some(supplier.name)
      if (update.contact.isDefined) update.contact ==== supplier.contact
      if (update.address.isDefined) update.address ==== supplier.address
      if (update.secondaryAddress.isDefined) update.secondaryAddress ==== supplier.secondaryAddress
      if (update.email.isDefined) update.email ==== supplier.email
      if (update.phoneNumber.isDefined) update.phoneNumber ==== supplier.phoneNumber
      if (update.secondaryPhoneNumber.isDefined)
        update.secondaryPhoneNumber ==== supplier.secondaryPhoneNumber
      if (update.accountNumber.isDefined) update.accountNumber ==== supplier.accountNumber
      if (update.notes.isDefined) update.notes ==== supplier.notes

      update.locationOverrides.foreach {
        case (locationId, Some(locationUpdate)) => assertItemLocationUpdate(supplierId, locationId, locationUpdate)
        case (locationId, None)                 => assertItemLocationDoesntExist(supplierId, locationId)
      }

      update.productIds.foreach { productIds =>
        val supplierProducts = supplierProductDao.findBySupplierId(supplierId).await
        supplierProducts.map(_.productId).toSet ==== productIds.toSet
      }
    }

    def assertResponse(
        entity: SupplierEntity,
        record: SupplierRecord,
        productsCount: Option[Int] = None,
        stockValue: Option[MonetaryAmount] = None,
        locationOverrides: Option[Map[UUID, ItemLocation]] = None,
      ) = {
      entity.id ==== record.id
      entity.name ==== record.name
      entity.contact ==== record.contact
      entity.address ==== record.address
      entity.secondaryAddress ==== record.secondaryAddress
      entity.email ==== record.email
      entity.phoneNumber ==== record.phoneNumber
      entity.secondaryPhoneNumber ==== record.secondaryPhoneNumber
      entity.accountNumber ==== record.accountNumber
      entity.notes ==== record.notes

      entity.productsCount ==== productsCount
      entity.stockValue ==== stockValue

      entity.productsCount ==== productsCount
      entity.stockValue ==== stockValue

      entity.locationOverrides ==== locationOverrides
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
