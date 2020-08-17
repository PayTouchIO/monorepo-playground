package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class InventoryCountsDeleteFSpec extends InventoryCountsFSpec {

  abstract class InventoryCountDeleteResourceFSpecContext extends InventoryCountResourceFSpecContext {
    def assertInventoryCountDoesntExist(id: UUID) = inventoryCountDao.findById(id).await should beNone
    def assertInventoryCountExists(id: UUID) = inventoryCountDao.findById(id).await should beSome
  }

  "POST /v1/inventory_counts.delete" in {

    "if request has valid token" in {
      "if inventory count doesn't exist" should {
        "do nothing and return 204" in new InventoryCountDeleteResourceFSpecContext {
          val nonExistingInventoryCountId = UUID.randomUUID

          Post(s"/v1/inventory_counts.delete", Ids(ids = Seq(nonExistingInventoryCountId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertInventoryCountDoesntExist(nonExistingInventoryCountId)
          }
        }
      }

      "if inventory count belongs to the merchant" should {
        "delete the inventory count and return 204" in new InventoryCountDeleteResourceFSpecContext {
          val simpleProduct = Factory.simpleProduct(merchant).create

          val inventoryCount1 = Factory.inventoryCount(rome, user).create
          Factory.inventoryCountProduct(inventoryCount1, simpleProduct).create

          val inventoryCount2 = Factory.inventoryCount(london, user).create
          Factory.inventoryCountProduct(inventoryCount2, simpleProduct).create

          val inventoryCount3 = Factory.inventoryCount(rome, user).create
          Factory.inventoryCountProduct(inventoryCount3, simpleProduct).create

          Post(s"/v1/inventory_counts.delete", Ids(ids = Seq(inventoryCount1.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertInventoryCountDoesntExist(inventoryCount1.id)
            inventoryCountProductDao.findByInventoryCountId(inventoryCount1.id).await ==== Seq.empty

            assertInventoryCountExists(inventoryCount2.id)
            assertInventoryCountExists(inventoryCount3.id)
          }
        }
      }

      "if inventory count belongs to a different merchant" should {
        "do not delete the inventoryCount and return 204" in new InventoryCountDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(merchant).create
          val competitorLocation = Factory.location(competitor).create
          val competitorInventoryCount = Factory.inventoryCount(competitorLocation, competitorUser).create

          Post(s"/v1/inventory_counts.delete", Ids(ids = Seq(competitorInventoryCount.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertInventoryCountExists(competitorInventoryCount.id)
          }
        }
      }
    }
  }
}
