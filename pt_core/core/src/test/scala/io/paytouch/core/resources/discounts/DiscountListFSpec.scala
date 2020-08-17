package io.paytouch.core.resources.discounts

import java.time.ZonedDateTime

import io.paytouch.core.entities.{ Discount => DiscountEntity, _ }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class DiscountListFSpec extends DiscountsFSpec {

  "GET /v1/discounts.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all discounts sorted by title" in new DiscountResourceFSpecContext {
          val discount1 = Factory.discount(merchant, title = Some("A discount")).create
          val discount2 = Factory.discount(merchant, title = Some("B discount"), locations = Seq(rome)).create
          val discount3 = Factory.discount(merchant, title = Some("C discount")).create

          Get("/v1/discounts.list").addHeader(authorizationHeader) ~> routes ~> check {
            val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]
            discounts.data.map(_.id) ==== Seq(discount1.id, discount2.id, discount3.id)
            assertResponse(discount1, discounts.data.find(_.id == discount1.id).get)
            assertResponse(discount2, discounts.data.find(_.id == discount2.id).get)
            assertResponse(discount3, discounts.data.find(_.id == discount3.id).get)
          }
        }
      }

      "with location_id parameter" should {
        "return a paginated list of all categories sorted by title" in new DiscountResourceFSpecContext {
          val discount1 = Factory.discount(merchant, title = Some("A discount"), locations = Seq(rome)).create
          val discount2 = Factory.discount(merchant, title = Some("B discount"), locations = Seq(rome, london)).create
          val discount3 = Factory.discount(merchant, title = Some("C discount"), locations = Seq(london)).create

          def assertAB(locationIds: String): Unit =
            Get(s"/v1/discounts.list?$locationIds").addHeader(authorizationHeader) ~> routes ~> check {
              val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]

              discounts.data.map(_.id) should containTheSameElementsAs(Seq(discount1, discount2).map(_.id))

              assertResponse(discount1, discounts.data.find(_.id == discount1.id).get)
              assertResponse(discount2, discounts.data.find(_.id == discount2.id).get)
            }

          def assertABC(locationIds: String): Unit =
            Get(s"/v1/discounts.list?$locationIds").addHeader(authorizationHeader) ~> routes ~> check {
              val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]

              discounts.data.map(_.id) should containTheSameElementsAs(Seq(discount1, discount2, discount3).map(_.id))

              assertResponse(discount1, discounts.data.find(_.id == discount1.id).get)
              assertResponse(discount2, discounts.data.find(_.id == discount2.id).get)
              assertResponse(discount3, discounts.data.find(_.id == discount3.id).get)
            }

          val londonId = london.id
          val romeId = rome.id

          assertAB(s"location_id=$romeId")
          assertAB(s"location_id[]=$romeId")
          assertAB(s"location_id=$romeId&location_id[]=$romeId")
          assertABC(s"location_id=$londonId&location_id[]=$romeId")
          assertABC(s"location_id[]=$romeId,$londonId")
        }
      }

      "with q parameter" should {
        "return a paginated list of all categories sorted by title" in new DiscountResourceFSpecContext {
          val discount1 = Factory.discount(merchant, title = Some("A discount")).create
          val discount2 = Factory.discount(merchant, title = Some("B discount")).create
          val discount3 = Factory.discount(merchant, title = Some("C discount")).create

          Get(s"/v1/discounts.list?q=B").addHeader(authorizationHeader) ~> routes ~> check {
            val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]
            discounts.data.map(_.id) ==== Seq(discount2.id)
            assertResponse(discount2, discounts.data.find(_.id == discount2.id).get)
          }
        }
      }

      "with updated_since date-time" should {
        "return a paginated list of all categories sorted by title filtered by updated_since" in new DiscountResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val discount1 =
            Factory.discount(merchant, title = Some("A discount"), overrideNow = Some(now.minusDays(1))).create
          val discount2 = Factory.discount(merchant, title = Some("B discount"), overrideNow = Some(now)).create
          val discount3 =
            Factory.discount(merchant, title = Some("C discount"), overrideNow = Some(now.plusDays(1))).create

          Get(s"/v1/discounts.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]
            discounts.data.map(_.id) ==== Seq(discount2.id, discount3.id)
            assertResponse(discount2, discounts.data.find(_.id == discount2.id).get)
            assertResponse(discount3, discounts.data.find(_.id == discount3.id).get)
          }
        }
      }

      "with expand[]=locations" should {
        "return a paginated list of all categories sorted by title" in new DiscountResourceFSpecContext {
          val discount1 = Factory.discount(merchant, title = Some("A discount")).create

          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create

          Factory.discountLocation(discount1, london, active = Some(true)).create
          Factory.discountLocation(discount1, rome, active = Some(false)).create
          Factory.discountLocation(discount1, deletedLocation, active = Some(false)).create

          val discount2 = Factory.discount(merchant, title = Some("B discount"), locations = Seq(london)).create
          val discount3 = Factory.discount(merchant, title = Some("C discount")).create

          Get(s"/v1/discounts.list?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]
            discounts.data.map(_.id) ==== Seq(discount1.id, discount2.id, discount3.id)
            assertResponse(
              discount1,
              discounts.data.find(_.id == discount1.id).get,
              locations = Some(Seq(london, rome)),
            )
            assertResponse(discount2, discounts.data.find(_.id == discount2.id).get, locations = Some(Seq(london)))
            assertResponse(discount3, discounts.data.find(_.id == discount3.id).get, locations = Some(Seq.empty))
          }
        }
      }

      "with expand[]=availabilities" should {
        "return discounts with availabilities expanded only for accessible locations" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant).create
          val newYork = Factory.location(merchant).create

          val discountAvailability =
            Factory.discountAvailability(discount, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val discountRome = Factory.discountLocation(discount, rome, active = Some(true)).create
          val discountLondon = Factory.discountLocation(discount, london, active = Some(false)).create
          val discountNewYork = Factory.discountLocation(discount, newYork, active = Some(true)).create

          val expectedAvailabilityMap = Map(
            Weekdays.Monday -> Seq(Availability(discountAvailability.start, discountAvailability.end)),
            Weekdays.Tuesday -> Seq(Availability(discountAvailability.start, discountAvailability.end)),
          )

          Get(s"/v1/discounts.list?expand[]=availabilities").addHeader(authorizationHeader) ~> routes ~> check {
            val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]
            discounts.data.map(_.id) ==== Seq(discount.id)

            val availabilities = discounts.data.map(_.availabilityHours.get)
            availabilities ==== Seq(expectedAvailabilityMap)
          }
        }
      }

      "with expand[]=locations and location_id filter" should {
        "return a paginated list of all categories sorted by title" in new DiscountResourceFSpecContext {
          val discount1 = Factory.discount(merchant, title = Some("A discount"), locations = Seq(rome, london)).create
          val discount2 = Factory.discount(merchant, title = Some("B discount"), locations = Seq(london)).create
          val discount3 = Factory.discount(merchant, title = Some("C discount")).create

          Get(s"/v1/discounts.list?expand[]=locations&location_id=${london.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val discounts = responseAs[PaginatedApiResponse[Seq[DiscountEntity]]]
            discounts.data.map(_.id) ==== Seq(discount1.id, discount2.id)
            assertResponse(discount1, discounts.data.find(_.id == discount1.id).get, locations = Some(Seq(london)))
            assertResponse(discount2, discounts.data.find(_.id == discount2.id).get, locations = Some(Seq(london)))
          }
        }
      }
    }
  }

}
