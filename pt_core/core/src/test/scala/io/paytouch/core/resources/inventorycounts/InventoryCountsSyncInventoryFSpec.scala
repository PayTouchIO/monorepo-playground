package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class InventoryCountsSyncInventoryFSpec extends InventoryCountsFSpec {

  abstract class InventoryCountsSyncInventoryFSpecContext extends InventoryCountResourceFSpecContext {
    val stockDao = daos.stockDao
  }

  "POST /v1/inventory_counts.sync_inventory" in {
    "if request has valid token" in {

      "when count does not match" should {
        "mark the inventory count as synced, update the status and create stocks if non-existing" in new InventoryCountsSyncInventoryFSpecContext {
          val originalQuantity: BigDecimal = 10
          val overriddenQuantity: BigDecimal = 5

          val bag = Factory.simpleProduct(merchant).create
          val bagRome = Factory.productLocation(bag, rome).create

          val tShirt = Factory.simpleProduct(merchant).create
          val tShirtRome = Factory.productLocation(tShirt, rome).create

          val inventoryCount = Factory.inventoryCount(rome, user).create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              bag,
              countedQuantity = Some(overriddenQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              tShirt,
              countedQuantity = Some(originalQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create

          Post(s"/v1/inventory_counts.sync_inventory?inventory_count_id=${inventoryCount.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val inventoryCountResponse = responseAs[ApiResponse[InventoryCount]].data
            assertResponse(
              inventoryCountResponse,
              inventoryCount.copy(synced = true, status = InventoryCountStatus.Unmatched),
            )
            val stocks = stockDao.findByProductIdsAndLocationIds(Seq(bag.id, tShirt.id), Seq(rome.id)).await
            stocks.find(s => s.productId == bag.id).get.quantity ==== overriddenQuantity
            stocks.find(s => s.productId == tShirt.id).get.quantity ==== originalQuantity
          }
        }

        "mark the inventory count as synced, update the status and update stocks if existing" in new InventoryCountsSyncInventoryFSpecContext {
          val originalQuantity: BigDecimal = 10
          val overriddenQuantity: BigDecimal = 5

          val bag = Factory.simpleProduct(merchant).create
          val bagRome = Factory.productLocation(bag, rome).create
          val bagRomeStock = Factory.stock(bagRome, quantity = Some(originalQuantity)).create

          val tShirt = Factory.simpleProduct(merchant).create
          val tShirtRome = Factory.productLocation(tShirt, rome).create
          val tShirtRomeStock = Factory.stock(tShirtRome, quantity = Some(originalQuantity)).create

          val inventoryCount = Factory.inventoryCount(rome, user).create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              bag,
              countedQuantity = Some(overriddenQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              tShirt,
              countedQuantity = Some(originalQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create

          Post(s"/v1/inventory_counts.sync_inventory?inventory_count_id=${inventoryCount.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val inventoryCountResponse = responseAs[ApiResponse[InventoryCount]].data
            assertResponse(
              inventoryCountResponse,
              inventoryCount.copy(synced = true, status = InventoryCountStatus.Unmatched),
            )
            stockDao.findById(bagRomeStock.id).await.get.quantity ==== overriddenQuantity
            stockDao.findById(tShirtRomeStock.id).await.get.quantity ==== originalQuantity
          }
        }
      }

      "when count matches" should {
        "mark the inventory count as synced, update the status and create stocks if non-existing" in new InventoryCountsSyncInventoryFSpecContext {
          val originalQuantity: BigDecimal = 10

          val bag = Factory.simpleProduct(merchant).create
          val bagRome = Factory.productLocation(bag, rome).create
          val bagRomeStock = Factory.stock(bagRome, quantity = Some(originalQuantity)).create

          val tShirt = Factory.simpleProduct(merchant).create
          val tShirtRome = Factory.productLocation(tShirt, rome).create

          val inventoryCount = Factory.inventoryCount(rome, user).create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              bag,
              countedQuantity = Some(originalQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              tShirt,
              countedQuantity = Some(originalQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create

          Post(s"/v1/inventory_counts.sync_inventory?inventory_count_id=${inventoryCount.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val inventoryCountResponse = responseAs[ApiResponse[InventoryCount]].data
            assertResponse(
              inventoryCountResponse,
              inventoryCount.copy(synced = true, status = InventoryCountStatus.Matched),
            )
            val stocks = stockDao.findByProductIdsAndLocationIds(Seq(bag.id, tShirt.id), Seq(rome.id)).await
            stocks.find(s => s.productId == bag.id).get.quantity ==== originalQuantity
            stocks.find(s => s.productId == tShirt.id).get.quantity ==== originalQuantity
          }
        }

        "mark the inventory count as synced, update the status and update stocks if existing" in new InventoryCountsSyncInventoryFSpecContext {
          val originalQuantity: BigDecimal = 10

          val bag = Factory.simpleProduct(merchant).create
          val bagRome = Factory.productLocation(bag, rome).create
          val bagRomeStock = Factory.stock(bagRome, quantity = Some(originalQuantity)).create

          val tShirt = Factory.simpleProduct(merchant).create
          val tShirtRome = Factory.productLocation(tShirt, rome).create
          val tShirtRomeStock = Factory.stock(tShirtRome, quantity = Some(originalQuantity)).create

          val inventoryCount = Factory.inventoryCount(rome, user).create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              bag,
              countedQuantity = Some(originalQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create
          Factory
            .inventoryCountProduct(
              inventoryCount,
              tShirt,
              countedQuantity = Some(originalQuantity),
              expectedQuantity = Some(originalQuantity),
            )
            .create

          Post(s"/v1/inventory_counts.sync_inventory?inventory_count_id=${inventoryCount.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val inventoryCountResponse = responseAs[ApiResponse[InventoryCount]].data
            assertResponse(
              inventoryCountResponse,
              inventoryCount.copy(synced = true, status = InventoryCountStatus.Matched),
            )
            stockDao.findById(bagRomeStock.id).await.get.quantity ==== originalQuantity
            stockDao.findById(tShirtRomeStock.id).await.get.quantity ==== originalQuantity
          }
        }
      }

      "if inventory count is already being synced" should {
        "return 400" in new InventoryCountsSyncInventoryFSpecContext {
          val inventoryCount =
            Factory.inventoryCount(rome, user, status = Some(InventoryCountStatus.Matched), synced = Some(true)).create

          Post(s"/v1/inventory_counts.sync_inventory?inventory_count_id=${inventoryCount.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new InventoryCountsSyncInventoryFSpecContext {
        val newInventoryCountId = UUID.randomUUID
        Post(s"/v1/inventory_counts.sync_inventory?inventory_count_id=$newInventoryCountId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
