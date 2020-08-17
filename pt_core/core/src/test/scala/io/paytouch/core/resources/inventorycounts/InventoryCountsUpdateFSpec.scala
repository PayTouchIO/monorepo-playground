package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class InventoryCountsUpdateFSpec extends InventoryCountsFSpec {

  abstract class InventoryCountsUpdateFSpecContext extends InventoryCountResourceFSpecContext

  "POST /v1/inventory_counts.update" in {
    "if request has valid token" in {

      "update inventory count and return 201" in new InventoryCountsUpdateFSpecContext {
        val inventoryCount = Factory.inventoryCount(rome, user, status = Some(InventoryCountStatus.InProgress)).create

        val product = Factory.simpleProduct(merchant).create
        val productLocation = Factory.productLocation(product, rome, costAmount = Some(6)).create

        val inventoryCountProduct = InventoryCountProductUpsertion(
          productId = product.id,
          expectedQuantity = Some(3.0),
          countedQuantity = Some(2.0),
        )
        val update =
          random[InventoryCountUpdate].copy(locationId = rome.id, products = Some(Seq(inventoryCountProduct)))
        val productValueChangeAmount = productLocation.costAmount.get * -1.0

        val productExpectations = Seq(
          InventoryCountProductExpectation(
            productId = product.id,
            productName = product.name,
            valueAmount = Some(12.0),
            costAmount = productLocation.costAmount,
            valueChangeAmount = Some(productValueChangeAmount),
          ),
        )

        Post(s"/v1/inventory_counts.update?inventory_count_id=${inventoryCount.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val inventoryCount = responseAs[ApiResponse[InventoryCount]].data
          assertUpdate(
            inventoryCount.id,
            update,
            expectedNumber = inventoryCount.number,
            expectedValueChangeAmount = productValueChangeAmount,
            expectedStatus = InventoryCountStatus.InProgress,
            expectedSynced = false,
            productExpectations = productExpectations,
          )
        }
      }
    }

    "if request has an invalid product id" should {
      "return 404" in new InventoryCountsUpdateFSpecContext {
        val inventoryCount = Factory.inventoryCount(rome, user, status = Some(InventoryCountStatus.InProgress)).create

        val inventoryCountProduct = InventoryCountProductUpsertion(
          productId = UUID.randomUUID,
          expectedQuantity = Some(3.0),
          countedQuantity = Some(2.0),
        )

        val update =
          random[InventoryCountUpdate].copy(locationId = rome.id, products = Some(Seq(inventoryCountProduct)))

        Post(s"/v1/inventory_counts.update?inventory_count_id=${inventoryCount.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid location id" should {
      "return 404" in new InventoryCountsUpdateFSpecContext {
        val inventoryCount = Factory.inventoryCount(rome, user, status = Some(InventoryCountStatus.InProgress)).create
        val product = Factory.simpleProduct(merchant, costAmount = Some(6)).create

        val update =
          random[InventoryCountUpdate].copy(locationId = UUID.randomUUID, products = None)

        Post(s"/v1/inventory_counts.update?inventory_count_id=${inventoryCount.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if inventory count has already been synced" should {
      "return 400" in new InventoryCountsUpdateFSpecContext {
        val inventoryCount = Factory.inventoryCount(rome, user, synced = Some(true)).create

        val update =
          random[InventoryCountUpdate].copy(locationId = rome.id, products = None)
        Post(s"/v1/inventory_counts.update?inventory_count_id=${inventoryCount.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new InventoryCountsUpdateFSpecContext {
        val newInventoryCountId = UUID.randomUUID
        val update = random[InventoryCountUpdate]
        Post(s"/v1/inventory_counts.update?inventory_count_id=$newInventoryCountId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
