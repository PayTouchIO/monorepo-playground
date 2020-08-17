package io.paytouch.core.resources.groups

import java.time.{ ZoneOffset, ZonedDateTime }

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Group => GroupEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GroupsListFSpec extends GroupsFSpec {

  abstract class GroupListResourceFSpecContext extends GroupResourceFSpecContext

  "GET /v1/groups.list" in {
    "if request has valid token" in {
      "with no parameters" should {
        "return list of all groups ordered by name" in new GroupListResourceFSpecContext {
          val groupFriends = Factory.group(merchant, name = Some("Friends")).create
          val groupEnemies = Factory.group(merchant, name = Some("Enemies")).create

          Get(s"/v1/groups.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(groupEnemies.id, groupFriends.id)

            assertResponse(groups.head, groupEnemies)
            assertResponse(groups(1), groupFriends)
          }
        }
      }
      "with expand[]=customers,customers_count" should {
        "return list of all groups with customers expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer(merchant = Some(merchant)).create
          val globalCustomer2 = Factory.globalCustomer(merchant = Some(merchant)).create

          val newyork = Factory.location(merchant).create
          val globalCustomer3 = Factory.globalCustomer(merchant = Some(merchant)).create

          Factory.customerLocation(globalCustomer1, rome).create
          Factory.customerLocation(globalCustomer2, rome).create
          // globalCustomer3 belongs to a location which the current user can't access, so it won't appear in results
          Factory.customerLocation(globalCustomer3, newyork).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.list?expand[]=customers,customers_count")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(
              groups.head,
              group,
              customerIds = Seq(globalCustomer1.id, globalCustomer2.id),
              customersCount = Some(2),
            )
          }
        }
      }
      "with expand[]=customers,customers_count and location_id" should {
        "return list of all groups with customers in a location expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1 = Factory.customerMerchant(merchant, globalCustomer1).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2 = Factory.customerMerchant(merchant, globalCustomer2).create

          Factory.customerLocation(globalCustomer1, rome).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.list?expand[]=customers,customers_count&location_id=${rome.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, customerIds = Seq(globalCustomer1.id), customersCount = Some(2))
          }
        }
      }
      "with expand[]=customers,customers_count and from date" should {
        "return list of all groups with customers created after date expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1CreatedAt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer1 =
            Factory.customerMerchant(merchant, globalCustomer1, overrideNow = Some(customer1CreatedAt)).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2CreatedAt = ZonedDateTime.of(2016, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer2 =
            Factory.customerMerchant(merchant, globalCustomer2, overrideNow = Some(customer2CreatedAt)).create

          Factory.customerLocation(globalCustomer1, rome).create
          Factory.customerLocation(globalCustomer2, rome).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.list?expand[]=customers,customers_count&from=2016-02-23")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, customerIds = Seq(globalCustomer2.id), customersCount = Some(2))
          }
        }
      }
      "with expand[]=customers,customers_count and to date" should {
        "return list of all groups with customers created after date expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1CreatedAt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer1 =
            Factory.customerMerchant(merchant, globalCustomer1, overrideNow = Some(customer1CreatedAt)).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2CreatedAt = ZonedDateTime.of(2016, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer2 =
            Factory.customerMerchant(merchant, globalCustomer2, overrideNow = Some(customer2CreatedAt)).create

          Factory.customerLocation(globalCustomer1, rome).create
          Factory.customerLocation(globalCustomer2, rome).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.list?expand[]=customers,customers_count&to=2016-02-23")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, customerIds = Seq(globalCustomer1.id), customersCount = Some(2))
          }
        }
      }
      "with expand[]=revenue" should {
        "return list of all groups with revenues expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1 = Factory.customerMerchant(merchant, globalCustomer1).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2 = Factory.customerMerchant(merchant, globalCustomer2).create

          val newyork = Factory.location(merchant).create
          val globalCustomer3 = Factory.globalCustomer().create
          val customer3 = Factory.customerMerchant(merchant, globalCustomer3).create

          Factory.customerLocation(globalCustomer1, rome, totalSpend = Some(7)).create
          Factory.customerLocation(globalCustomer2, rome, totalSpend = Some(3)).create
          // customer3 belongs to a location which the current user can't access, so it won't appear in results
          Factory.customerLocation(globalCustomer3, newyork).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.list?expand[]=revenue").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, revenues = Seq(10.$$$))
          }
        }
      }
      "with expand[]=revenue and location_id and date" should {
        "return list of all groups with revenues expanded filtered by location_id and date" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1CreatedAt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer1 =
            Factory.customerMerchant(merchant, globalCustomer1, overrideNow = Some(customer1CreatedAt)).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2CreatedAt = ZonedDateTime.of(2016, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer2 =
            Factory.customerMerchant(merchant, globalCustomer2, overrideNow = Some(customer2CreatedAt)).create
          val globalCustomer3 = Factory.globalCustomer().create
          val customer3CreatedAt = ZonedDateTime.of(2016, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer3 =
            Factory.customerMerchant(merchant, globalCustomer3, overrideNow = Some(customer3CreatedAt)).create

          Factory.customerLocation(globalCustomer1, rome, totalSpend = Some(1)).create
          Factory.customerLocation(globalCustomer2, london, totalSpend = Some(2)).create
          Factory.customerLocation(globalCustomer3, london, totalSpend = Some(4)).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create
          Factory.customerGroup(globalCustomer3, group).create

          Get(s"/v1/groups.list?expand[]=revenue&location_id=${london.id}&from=2016-03-24")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, revenues = Seq(4.$$$))
          }
        }
      }
      "with expand[]=visits" should {
        "return list of all groups with visits expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1 = Factory.customerMerchant(merchant, globalCustomer1).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2 = Factory.customerMerchant(merchant, globalCustomer2).create

          val newyork = Factory.location(merchant).create
          val globalCustomer3 = Factory.globalCustomer().create
          val customer3 = Factory.customerMerchant(merchant, globalCustomer3).create

          Factory.customerLocation(globalCustomer1, rome, totalVisits = Some(7)).create
          Factory.customerLocation(globalCustomer2, rome, totalVisits = Some(3)).create
          // customer3 belongs to a location which the current user can't access, so it won't appear in results
          Factory.customerLocation(globalCustomer3, newyork).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.list?expand[]=visits").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, visits = Some(10))
          }
        }
      }
      "with expand[]=visits and location_id and from date" should {
        "return list of all groups with visits expanded filtered by location_id and from date" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1CreatedAt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer1 =
            Factory.customerMerchant(merchant, globalCustomer1, overrideNow = Some(customer1CreatedAt)).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2CreatedAt = ZonedDateTime.of(2016, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer2 =
            Factory.customerMerchant(merchant, globalCustomer2, overrideNow = Some(customer2CreatedAt)).create
          val globalCustomer3 = Factory.globalCustomer().create
          val customer3CreatedAt = ZonedDateTime.of(2016, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer3 =
            Factory.customerMerchant(merchant, globalCustomer3, overrideNow = Some(customer3CreatedAt)).create

          Factory.customerLocation(globalCustomer1, rome, totalVisits = Some(1)).create
          Factory.customerLocation(globalCustomer2, london, totalVisits = Some(2)).create
          Factory.customerLocation(globalCustomer3, london, totalVisits = Some(4)).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create
          Factory.customerGroup(globalCustomer3, group).create

          Get(s"/v1/groups.list?expand[]=visits&location_id=${london.id}&from=2016-03-24")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group, visits = Some(4))
          }
        }
      }
      "if groups has no customers" should {
        "return list of all groups with visits expanded" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create

          Get(s"/v1/groups.list?expand[]=customers,customers_count,revenue,visits")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(
              groups.head,
              group,
              customerIds = Seq.empty,
              customersCount = Some(0),
              revenues = Seq.empty,
              visits = Some(0),
            )
          }
        }
      }
      "with filter location_id" should {
        "return list of all groups with visits expanded filtered by location_id" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer(merchant = Some(merchant)).create
          val globalCustomer2 = Factory.globalCustomer(merchant = Some(merchant)).create
          val globalCustomer3 = Factory.globalCustomer(merchant = Some(merchant)).create

          Factory.customerLocation(globalCustomer1, rome, totalVisits = Some(1)).create
          Factory.customerLocation(globalCustomer2, london, totalVisits = Some(2)).create
          Factory.customerLocation(globalCustomer3, london, totalVisits = Some(4)).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create
          Factory.customerGroup(globalCustomer3, group).create

          Get(s"/v1/groups.list?location_id=${london.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group)
          }
        }
      }
      "with filter from date" should {
        "return list of all groups with visits expanded filtered by from date" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1CreatedAt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer1 =
            Factory.customerMerchant(merchant, globalCustomer1, overrideNow = Some(customer1CreatedAt)).create

          val globalCustomer2 = Factory.globalCustomer().create
          val customer2CreatedAt = ZonedDateTime.of(2016, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer2 =
            Factory.customerMerchant(merchant, globalCustomer2, overrideNow = Some(customer2CreatedAt)).create

          val globalCustomer3 = Factory.globalCustomer().create
          val customer3CreatedAt = ZonedDateTime.of(2016, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer3 =
            Factory.customerMerchant(merchant, globalCustomer3, overrideNow = Some(customer3CreatedAt)).create

          Factory.customerLocation(globalCustomer1, rome, totalVisits = Some(1)).create
          Factory.customerLocation(globalCustomer2, london, totalVisits = Some(2)).create
          Factory.customerLocation(globalCustomer3, london, totalVisits = Some(4)).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create
          Factory.customerGroup(globalCustomer3, group).create

          Get(s"/v1/groups.list?from=2016-03-24").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group)
          }
        }
      }
      "with filter to date" should {
        "return list of all groups with visits expanded filtered by to date" in new GroupListResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1CreatedAt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer1 =
            Factory.customerMerchant(merchant, globalCustomer1, overrideNow = Some(customer1CreatedAt)).create

          val globalCustomer2 = Factory.globalCustomer().create
          val customer2CreatedAt = ZonedDateTime.of(2016, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer2 =
            Factory.customerMerchant(merchant, globalCustomer2, overrideNow = Some(customer2CreatedAt)).create

          val globalCustomer3 = Factory.globalCustomer().create
          val customer3CreatedAt = ZonedDateTime.of(2016, 4, 15, 0, 0, 0, 0, ZoneOffset.UTC)
          val customer3 =
            Factory.customerMerchant(merchant, globalCustomer3, overrideNow = Some(customer3CreatedAt)).create

          Factory.customerLocation(globalCustomer1, rome, totalVisits = Some(1)).create
          Factory.customerLocation(globalCustomer2, london, totalVisits = Some(2)).create
          Factory.customerLocation(globalCustomer3, london, totalVisits = Some(4)).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create
          Factory.customerGroup(globalCustomer3, group).create

          Get(s"/v1/groups.list?to=2016-03-24").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(group.id)

            assertResponse(groups.head, group)
          }
        }
      }
      "with filter query" should {
        "return list of all groups with visits expanded filtered by query" in new GroupListResourceFSpecContext {
          val gold = Factory.group(merchant, name = Some("Gold")).create
          val silver = Factory.group(merchant, name = Some("Silver")).create
          val bronze = Factory.group(merchant, name = Some("Bronze")).create

          Get(s"/v1/groups.list?q=gol").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groups = responseAs[PaginatedApiResponse[Seq[GroupEntity]]].data
            groups.map(_.id) ==== Seq(gold.id)

            assertResponse(groups.head, gold)
          }
        }
      }
    }
  }
}
