package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class InventoryCountsGetFSpec extends InventoryCountsFSpec {

  abstract class InventoryCountsGetFSpecContext extends InventoryCountResourceFSpecContext

  "GET /v1/inventory_counts.get?inventory_count_id=$" in {
    "if request has valid token" in {

      "if the inventory count exists" should {

        "with no parameters" should {
          "return the inventory count" in new InventoryCountsGetFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product2 = Factory.simpleProduct(merchant).create
            val inventoryCountRecord = Factory.inventoryCount(london, user).create
            Factory.inventoryCountProduct(inventoryCountRecord, product1).create
            Factory.inventoryCountProduct(inventoryCountRecord, product2).create

            Get(s"/v1/inventory_counts.get?inventory_count_id=${inventoryCountRecord.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[InventoryCount]].data
              assertResponse(entity, inventoryCountRecord, productsCount = Some(2))
            }
          }
        }

        "with expand[]=location" should {
          "return the inventory count" in new InventoryCountsGetFSpecContext {
            val inventoryCountRecord = Factory.inventoryCount(london, user).create

            Get(s"/v1/inventory_counts.get?inventory_count_id=${inventoryCountRecord.id}&expand[]=location")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[InventoryCount]].data
              assertResponse(entity, inventoryCountRecord, location = Some(london))
            }
          }
        }

        "with expand[]=user" should {
          "return the inventory count" in new InventoryCountsGetFSpecContext {
            val inventoryCountRecord = Factory.inventoryCount(london, user).create

            Get(s"/v1/inventory_counts.get?inventory_count_id=${inventoryCountRecord.id}&expand[]=user")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entity = responseAs[ApiResponse[InventoryCount]].data
              assertResponse(entity, inventoryCountRecord, user = Some(user))
            }
          }
        }
      }

      "if the inventory count does not belong to the merchant" should {
        "return 404" in new InventoryCountsGetFSpecContext {
          val competitor = Factory.merchant.create
          val locationCompetitor = Factory.location(competitor).create
          val userCompetitor = Factory.user(competitor).create
          val inventoryCountCompetitor =
            Factory.inventoryCount(locationCompetitor, userCompetitor).create

          Get(s"/v1/inventory_counts.get?inventory_count_id=${inventoryCountCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the inventory count does not exist" should {
        "return 404" in new InventoryCountsGetFSpecContext {
          Get(s"/v1/inventory_counts.get?inventory_count_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
