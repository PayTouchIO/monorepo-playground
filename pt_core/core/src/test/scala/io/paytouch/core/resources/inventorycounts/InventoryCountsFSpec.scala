package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.entities.{
  InventoryCountCreation,
  InventoryCountProductUpsertion,
  InventoryCountUpdate,
  MonetaryAmount,
  InventoryCount => InventoryCountEntity,
}
import io.paytouch.core.utils._

// Contains expectations for all calculated fields
final case class InventoryCountProductExpectation(
    productId: UUID,
    productName: String,
    valueAmount: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    valueChangeAmount: Option[BigDecimal],
  )

abstract class InventoryCountsFSpec extends FSpec {

  abstract class InventoryCountResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val inventoryCountDao = daos.inventoryCountDao
    val inventoryCountProductDao = daos.inventoryCountProductDao
    val productDao = daos.productDao

    def assertResponseById(
        entity: InventoryCountEntity,
        recordId: UUID,
        location: Option[LocationRecord] = None,
        user: Option[UserRecord] = None,
      ) = {
      val record = inventoryCountDao.findById(recordId).await.get
      assertResponse(entity, record, location = location, user = user)
    }

    def assertResponse(
        entity: InventoryCountEntity,
        record: InventoryCountRecord,
        productsCount: Option[Int] = None,
        location: Option[LocationRecord] = None,
        user: Option[UserRecord] = None,
      ) = {
      entity.id ==== record.id
      entity.number ==== record.number
      entity.valueChange ==== MonetaryAmount.extract(record.valueChangeAmount, currency)
      entity.status ==== record.status
      entity.synced ==== record.synced
      entity.createdAt ==== record.createdAt

      if (productsCount.isDefined) productsCount ==== Some(entity.productsCount)

      entity.location.map(_.id) ==== location.map(_.id)
      entity.user.map(_.id) ==== user.map(_.id)
    }

    def assertCreation(
        id: UUID,
        creation: InventoryCountCreation,
        expectedNumber: String,
        expectedValueChangeAmount: BigDecimal,
        expectedStatus: InventoryCountStatus,
        expectedSynced: Boolean,
        productExpectations: Seq[InventoryCountProductExpectation],
      ) =
      assertUpdate(
        id,
        creation.asUpdate,
        expectedNumber = expectedNumber,
        expectedValueChangeAmount = expectedValueChangeAmount,
        expectedStatus = expectedStatus,
        expectedSynced = expectedSynced,
        productExpectations = productExpectations,
      )

    def assertUpdate(
        id: UUID,
        update: InventoryCountUpdate,
        expectedNumber: String,
        expectedValueChangeAmount: BigDecimal,
        expectedStatus: InventoryCountStatus,
        expectedSynced: Boolean,
        productExpectations: Seq[InventoryCountProductExpectation],
      ) = {
      val record = inventoryCountDao.findById(id).await.get

      update.locationId ==== record.locationId

      record.number ==== expectedNumber
      record.valueChangeAmount ==== Some(expectedValueChangeAmount)
      record.status ==== expectedStatus
      record.synced ==== expectedSynced

      update.products.map { inventoryCountProducts =>
        val products = inventoryCountProductDao.findByInventoryCountId(id).await
        products.size ==== inventoryCountProducts.size

        inventoryCountProducts.map(inventoryCountProduct =>
          assertInventoryCountProduct(
            id,
            inventoryCountProduct,
            productExpectations.find(_.productId == inventoryCountProduct.productId).get,
          ),
        )
      }
    }

    def assertInventoryCountProduct(
        inventoryCountId: UUID,
        update: InventoryCountProductUpsertion,
        expectation: InventoryCountProductExpectation,
      ) = {
      val record =
        inventoryCountProductDao.findOneByInventoryCountIdAndProductId(inventoryCountId, update.productId).await.get

      record.productId ==== update.productId
      record.inventoryCountId ==== inventoryCountId
      record.expectedQuantity ==== update.expectedQuantity
      record.countedQuantity ==== update.countedQuantity
      record.productName ==== expectation.productName
      record.valueAmount ==== expectation.valueAmount
      record.costAmount ==== expectation.costAmount
      record.valueChangeAmount ==== expectation.valueChangeAmount
    }
  }
}
