package io.paytouch.core.resources.inventorycounts

import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.entities.{ PaginatedApiResponse, InventoryCount => InventoryCountEntity }
import io.paytouch.core.utils.{ Formatters, UtcTime, FixtureDaoFactory => Factory }

class InventoryCountsListFSpec extends InventoryCountsFSpec {

  trait InventoryCountsListFspecContext extends InventoryCountResourceFSpecContext

  "GET /v1/inventory_counts.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all inventory counts" in new InventoryCountsListFspecContext {
          val inventoryCount1 = Factory.inventoryCount(rome, user).create
          val inventoryCount2 = Factory.inventoryCount(rome, user).create
          val inventoryCount3 = Factory.inventoryCount(rome, user).create
          val inventoryCount4 = Factory.inventoryCount(rome, user).create

          Get("/v1/inventory_counts.list").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(
              inventoryCount1.id,
              inventoryCount2.id,
              inventoryCount3.id,
              inventoryCount4.id,
            )
            assertResponse(entities.data.find(_.id == inventoryCount1.id).get, inventoryCount1)
            assertResponse(entities.data.find(_.id == inventoryCount2.id).get, inventoryCount2)
            assertResponse(entities.data.find(_.id == inventoryCount3.id).get, inventoryCount3)
            assertResponse(entities.data.find(_.id == inventoryCount4.id).get, inventoryCount4)
          }
        }
      }

      "with location_id filter" should {
        "return a paginated list of all inventory counts filtered by location_id" in new InventoryCountsListFspecContext {
          val inventoryCount1 = Factory.inventoryCount(rome, user).create
          val inventoryCount2 = Factory.inventoryCount(rome, user).create
          val inventoryCount3 = Factory.inventoryCount(london, user).create
          val inventoryCount4 = Factory.inventoryCount(london, user).create

          Get(s"/v1/inventory_counts.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount1.id, inventoryCount2.id)
            assertResponse(entities.data.find(_.id == inventoryCount1.id).get, inventoryCount1)
            assertResponse(entities.data.find(_.id == inventoryCount2.id).get, inventoryCount2)
          }
        }
      }

      "with from filter" should {
        "return a paginated list of all inventory counts filtered by from date" in new InventoryCountsListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val inventoryCount1 = Factory.inventoryCount(rome, user, overrideNow = Some(yesterday.minusDays(10))).create
          val inventoryCount2 = Factory.inventoryCount(rome, user, overrideNow = Some(yesterday.plusDays(1))).create

          Get(s"/v1/inventory_counts.list?from=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount2.id)
            assertResponse(entities.data.find(_.id == inventoryCount2.id).get, inventoryCount2)
          }
        }
      }

      "with to filter" should {
        "return a paginated list of all inventory counts filtered by to date" in new InventoryCountsListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val inventoryCount1 = Factory.inventoryCount(rome, user, overrideNow = Some(yesterday.minusDays(10))).create
          val inventoryCount2 = Factory.inventoryCount(rome, user, overrideNow = Some(yesterday.plusDays(1))).create

          Get(s"/v1/inventory_counts.list?to=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount1.id)
            assertResponse(entities.data.find(_.id == inventoryCount1.id).get, inventoryCount1)
          }
        }
      }

      "with q filter" should {
        "return a paginated list of all inventory counts filtered by a given query on inventory count number" in new InventoryCountsListFspecContext {
          val inventoryCount1 = Factory.inventoryCount(rome, user).create
          val inventoryCount2 = Factory.inventoryCount(rome, user).create

          Get(s"/v1/inventory_counts.list?q=2").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount2.id)
            assertResponse(entities.data.find(_.id == inventoryCount2.id).get, inventoryCount2)
          }
        }
      }

      "with status filter" should {
        "return a paginated list of all inventory counts filtered by a given status" in new InventoryCountsListFspecContext {
          val inventoryCount1 =
            Factory.inventoryCount(rome, user, status = Some(InventoryCountStatus.InProgress)).create
          val inventoryCount2 = Factory.inventoryCount(rome, user, status = Some(InventoryCountStatus.Matched)).create

          Get(s"/v1/inventory_counts.list?status=${InventoryCountStatus.Matched.entryName}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount2.id)
            assertResponse(entities.data.find(_.id == inventoryCount2.id).get, inventoryCount2)
          }
        }
      }

      "with expand[]=user" should {
        "return a paginated list of all inventory counts" in new InventoryCountsListFspecContext {
          val inventoryCount = Factory.inventoryCount(rome, user).create

          Get(s"/v1/inventory_counts.list?expand[]=user").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount.id)
            assertResponse(entities.data.find(_.id == inventoryCount.id).get, inventoryCount, user = Some(user))
          }
        }
      }

      "with expand[]=location" should {
        "return a paginated list of all inventory counts" in new InventoryCountsListFspecContext {
          val inventoryCount = Factory.inventoryCount(rome, user).create

          Get(s"/v1/inventory_counts.list?expand[]=location").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[InventoryCountEntity]]]
            entities.data.map(_.id) ==== Seq(inventoryCount.id)
            assertResponse(entities.data.find(_.id == inventoryCount.id).get, inventoryCount, location = Some(rome))
          }
        }
      }
    }
  }

}
