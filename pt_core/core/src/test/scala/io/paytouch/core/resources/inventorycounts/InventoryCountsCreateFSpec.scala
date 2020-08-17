package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums.{ InventoryCountStatus, NextNumberType, ScopeType }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class InventoryCountsCreateFSpec extends InventoryCountsFSpec {

  abstract class InventoryCountsCreateFSpecContext extends InventoryCountResourceFSpecContext {
    val nextNumberDao = daos.nextNumberDao

    def assertNextInventoryCountNumber(locationId: UUID, nextNumber: Int) = {
      val record = nextNumberDao
        .findByScopeAndType(Scope.fromLocationId(locationId), NextNumberType.InventoryCount)
        .await
      record should beSome[NextNumberRecord]
      record.get.nextVal ==== nextNumber
    }
  }

  "POST /v1/inventory_counts.create" in {
    "if request has valid token" in {

      "create inventory count and return 201" in new InventoryCountsCreateFSpecContext {
        val newInventoryCountId = UUID.randomUUID

        val product1 = Factory.simpleProduct(merchant).create
        val product1Location = Factory.productLocation(product1, rome, costAmount = Some(6)).create
        val product2 = Factory.simpleProduct(merchant).create
        val product2Location = Factory.productLocation(product2, rome, costAmount = Some(8)).create

        val inventoryCountProduct1 = InventoryCountProductUpsertion(
          productId = product1.id,
          expectedQuantity = Some(3.0),
          countedQuantity = Some(2.0),
        )
        val inventoryCountProduct2 = InventoryCountProductUpsertion(
          productId = product2.id,
          expectedQuantity = Some(2.0),
          countedQuantity = Some(5.0),
        )

        val creation = random[InventoryCountCreation]
          .copy(locationId = rome.id, products = Seq(inventoryCountProduct1, inventoryCountProduct2))

        val product1ValueChangeAmount = product1Location.costAmount.get * -1.0
        val product2ValueChangeAmount = product2Location.costAmount.get * 3.0
        val productExpectations = Seq(
          InventoryCountProductExpectation(
            productId = product1.id,
            productName = product1.name,
            valueAmount = Some(12.0),
            costAmount = product1Location.costAmount,
            valueChangeAmount = Some(product1ValueChangeAmount),
          ),
          InventoryCountProductExpectation(
            productId = product2.id,
            productName = product2.name,
            valueAmount = Some(40.0),
            costAmount = product2Location.costAmount,
            valueChangeAmount = Some(product2ValueChangeAmount),
          ),
        )

        Post(s"/v1/inventory_counts.create?inventory_count_id=$newInventoryCountId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val inventoryCount = responseAs[ApiResponse[InventoryCount]].data
          assertCreation(
            newInventoryCountId,
            creation,
            expectedNumber = "1",
            expectedValueChangeAmount = product1ValueChangeAmount + product2ValueChangeAmount,
            expectedStatus = InventoryCountStatus.InProgress,
            expectedSynced = false,
            productExpectations = productExpectations,
          )
          assertResponseById(inventoryCount, newInventoryCountId)
          assertNextInventoryCountNumber(rome.id, nextNumber = 2)
        }
      }
    }

    "if request has invalid location id" should {
      "return 404" in new InventoryCountsCreateFSpecContext {
        val newInventoryCountId = UUID.randomUUID
        val creation = random[InventoryCountCreation].copy(locationId = UUID.randomUUID, products = Seq.empty)
        Post(s"/v1/inventory_counts.create?inventory_count_id=$newInventoryCountId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }

      "if request has invalid product id" should {
        "return 404" in new InventoryCountsCreateFSpecContext {
          val newInventoryCountId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val inventoryCountProduct = random[InventoryCountProductUpsertion]

          val creation =
            random[InventoryCountCreation].copy(locationId = rome.id, products = Seq(inventoryCountProduct))

          Post(s"/v1/inventory_counts.create?inventory_count_id=$newInventoryCountId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if request has invalid token" should {
        "be rejected" in new InventoryCountsCreateFSpecContext {
          val newInventoryCountId = UUID.randomUUID
          val creation = random[InventoryCountCreation]
          Post(s"/v1/inventory_counts.create?inventory_count_id=$newInventoryCountId", creation)
            .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
            rejection should beAnInstanceOf[AuthenticationFailedRejection]
          }
        }
      }
    }
  }
}
