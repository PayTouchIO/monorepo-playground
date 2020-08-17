package io.paytouch.core.resources.modifiersets

import java.time.ZonedDateTime

import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ModifierSetsListFSpec extends ModifierSetsFSpec {
  abstract class ModifierSetsListFSpecContext extends ModifierSetResourceFSpecContext

  "GET /v1/modifier_sets.list" in {
    "if the request has a valid token" in {
      "with expand[]=products_count,locations" should {
        "return a paginated list of all modifier sets sorted by name with their options ordered by position and name" in new ModifierSetsListFSpecContext {
          val modifierSet2 = Factory.modifierSet(merchant, name = Some("B modifierSet")).create
          val modifierSet3 = Factory.modifierSet(merchant, name = Some("C modifierSet")).create
          val modifierSet1 = Factory.modifierSet(merchant, name = Some("A modifierSet")).create

          val product = Factory.simpleProduct(merchant).create
          Factory.modifierSetProduct(modifierSet1, product).create

          Factory.modifierSetLocation(modifierSet2, london).create
          Factory.modifierSetLocation(modifierSet2, rome).create

          val option1 = Factory.modifierOption(modifierSet1, name = Some("Ketchup"), position = Some(1)).create
          val option2 = Factory.modifierOption(modifierSet1, name = Some("Mayonnaise"), position = Some(2)).create
          val option3 = Factory.modifierOption(modifierSet1, name = Some("BBQ Sauce"), position = Some(2)).create

          Get("/v1/modifier_sets.list?expand[]=products_count,locations").addHeader(
            authorizationHeader,
          ) ~> routes ~> check {
            val parsedResponse = responseAs[PaginatedApiResponse[Seq[ModifierSet]]]
            val modifierSets = parsedResponse.data

            parsedResponse.pagination.totalCount ==== 3
            modifierSets.map(_.id) ==== Seq(modifierSet1.id, modifierSet2.id, modifierSet3.id)
            assertResponse(
              modifierSets.find(_.id == modifierSet1.id).get,
              productsCount = Some(1),
              locations = Some(Seq.empty),
              options = Some(Seq(option1, option3, option2)),
            )
            assertResponse(
              modifierSets.find(_.id == modifierSet2.id).get,
              productsCount = Some(0),
              locations = Some(Seq(london, rome)),
              options = Some(Seq.empty),
            )
            assertResponse(
              modifierSets.find(_.id == modifierSet3.id).get,
              productsCount = Some(0),
              locations = Some(Seq.empty),
              options = Some(Seq.empty),
            )
          }
        }
      }

      "with location_id filter and locations expansion" should {
        "return a paginated list of all modifier sets sorted by name and filtered by location" in new ModifierSetsListFSpecContext {
          val modifierSet1 =
            Factory.modifierSet(merchant, name = Some("Bar1 modifierSet"), locations = Seq(rome)).create
          val modifierSet2 =
            Factory.modifierSet(merchant, name = Some("Bar2 modifierSet"), locations = Seq(london, rome)).create
          val modifierSet3 =
            Factory.modifierSet(merchant, name = Some("Cool modifierSet"), locations = Seq(london, rome)).create

          Get(s"/v1/modifier_sets.list?location_id=${london.id}&expand[]=locations")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val parsedResponse = responseAs[PaginatedApiResponse[Seq[ModifierSet]]]
            val modifierSets = parsedResponse.data
            modifierSets.map(_.id) ==== Seq(modifierSet2.id, modifierSet3.id)
            assertResponse(modifierSets.find(_.id == modifierSet2.id).get, locations = Some(Seq(london)))
            assertResponse(modifierSets.find(_.id == modifierSet3.id).get, locations = Some(Seq(london)))
            parsedResponse.pagination.totalCount ==== 2
          }
        }
      }

      "with q and location id filters combined" should {
        "return a paginated list of all modifier sets sorted by name and filtered by location" in new ModifierSetsListFSpecContext {
          val modifierSet1 =
            Factory.modifierSet(merchant, name = Some("Bar1 modifierSet"), locations = Seq(rome)).create

          val modifierSet2 =
            Factory.modifierSet(merchant, name = Some("Bar2 modifierSet"), locations = Seq(london, rome)).create

          val modifierSet3 =
            Factory.modifierSet(merchant, name = Some("Cool modifierSet"), locations = Seq(london)).create

          def assertBar(locationIds: String): Unit =
            Get(s"/v1/modifier_sets.list?$locationIds&q=Bar")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val parsedResponse = responseAs[PaginatedApiResponse[Seq[ModifierSet]]]

              val modifierSets = parsedResponse.data

              modifierSets.map(_.id) should containTheSameElementsAs(Seq(modifierSet2.id))

              assertResponse(modifierSets.find(_.id == modifierSet2.id).get)

              parsedResponse.pagination.totalCount ==== 1
            }

          def assertCoolBar(locationIds: String): Unit =
            Get(s"/v1/modifier_sets.list?$locationIds&q=Bar")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val parsedResponse = responseAs[PaginatedApiResponse[Seq[ModifierSet]]]

              val modifierSets = parsedResponse.data

              modifierSets.map(_.id) should containTheSameElementsAs(Seq(modifierSet1, modifierSet2).map(_.id))

              assertResponse(modifierSets.find(_.id == modifierSet1.id).get)
              assertResponse(modifierSets.find(_.id == modifierSet2.id).get)

              parsedResponse.pagination.totalCount ==== 2
            }

          val londonId = london.id
          val romeId = rome.id

          assertBar(s"location_id=$londonId")
          assertBar(s"location_id[]=$londonId")
          assertBar(s"location_id=$londonId&location_id[]=$londonId")
          assertCoolBar(s"location_id=$londonId&location_id[]=$romeId")
          assertCoolBar(s"location_id[]=$romeId,$londonId")
        }
      }

      "filtered by updated_since date-time" should {
        "return a paginated list of all modifier sets sorted by name and filtered by updated_since date-time" in new ModifierSetsListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val modifierSet1 =
            Factory.modifierSet(merchant, name = Some("A modifierSet"), overrideNow = Some(now.minusDays(1))).create
          val modifierSet2 =
            Factory.modifierSet(merchant, name = Some("B modifierSet"), overrideNow = Some(now)).create
          val modifierSet3 =
            Factory.modifierSet(merchant, name = Some("C modifierSet"), overrideNow = Some(now.plusDays(1))).create

          Get(s"/v1/modifier_sets.list?updated_since=2015-12-03")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val modifierSets = responseAs[PaginatedApiResponse[Seq[ModifierSet]]].data
            modifierSets.map(_.id) ==== Seq(modifierSet2.id, modifierSet3.id)
            assertResponse(modifierSets.find(_.id == modifierSet2.id).get)
            assertResponse(modifierSets.find(_.id == modifierSet3.id).get)
          }
        }
      }

      "filtered by ids" should {
        "return a paginated list of all modifier sets sorted by name and filtered by updated_since date-time" in new ModifierSetsListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val modifierSet1 =
            Factory.modifierSet(merchant, name = Some("A modifierSet"), overrideNow = Some(now.minusDays(1))).create
          val modifierSet2 =
            Factory.modifierSet(merchant, name = Some("B modifierSet"), overrideNow = Some(now)).create
          val modifierSet3 =
            Factory.modifierSet(merchant, name = Some("C modifierSet"), overrideNow = Some(now.plusDays(1))).create

          Get(s"/v1/modifier_sets.list?id[]=${modifierSet1.id},${modifierSet2.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val modifierSets = responseAs[PaginatedApiResponse[Seq[ModifierSet]]].data
            modifierSets.map(_.id) ==== Seq(modifierSet1.id, modifierSet2.id)
            assertResponse(modifierSets.find(_.id == modifierSet1.id).get)
            assertResponse(modifierSets.find(_.id == modifierSet2.id).get)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ModifierSetsListFSpecContext {
        Get(s"/v1/modifier_sets.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
