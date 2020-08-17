package io.paytouch.core.resources.taxrates

import java.time.ZonedDateTime

import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TaxRatesListFSpec extends TaxRatesFSpec {

  abstract class TaxRatesListFSpecContext extends TaxRatesResourceFSpecContext

  "GET /v1/tax_rates.list" in {

    "if the request has a valid token" should {

      "with no parameters" in {
        "return a paginated list of all tax rates sorted by name" in new TaxRatesListFSpecContext {
          val taxRate1 = Factory.taxRate(merchant, name = Some("A taxRate")).create
          val taxRate2 = Factory.taxRate(merchant, name = Some("B taxRate")).create
          val taxRate3 = Factory.taxRate(merchant, name = Some("C taxRate")).create

          Get("/v1/tax_rates.list").addHeader(authorizationHeader) ~> routes ~> check {
            val parsedResponse = responseAs[PaginatedApiResponse[Seq[TaxRate]]]
            val taxRates = parsedResponse.data
            assertResponse(taxRates.head, taxRate1)
            assertResponse(taxRates(1), taxRate2)
            assertResponse(taxRates(2), taxRate3)
            parsedResponse.pagination.totalCount ==== 3
          }
        }
      }

      "with location_id filter" in {
        "return a paginated list of all tax rates sorted by name and filtered by location" in new TaxRatesListFSpecContext {
          val taxRate1 = Factory.taxRate(merchant, name = Some("A taxRate"), locations = Seq(rome)).create
          val taxRate2 = Factory.taxRate(merchant, name = Some("B taxRate"), locations = Seq(rome, london)).create
          val taxRate3 = Factory.taxRate(merchant, name = Some("C taxRate"), locations = Seq(london)).create

          def assertAB(locationIds: String): Unit =
            Get(s"/v1/tax_rates.list?$locationIds").addHeader(authorizationHeader) ~> routes ~> check {
              val taxRates = responseAs[PaginatedApiResponse[Seq[TaxRate]]]

              taxRates.data.map(_.id) should containTheSameElementsAs(Seq(taxRate1, taxRate2).map(_.id))

              taxRates.pagination.totalCount ==== 2
            }

          def assertABC(locationIds: String): Unit =
            Get(s"/v1/tax_rates.list?$locationIds").addHeader(authorizationHeader) ~> routes ~> check {
              val taxRates = responseAs[PaginatedApiResponse[Seq[TaxRate]]]

              taxRates.data.map(_.id) should containTheSameElementsAs(Seq(taxRate1, taxRate2, taxRate3).map(_.id))

              taxRates.pagination.totalCount ==== 3
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

      "with updated_since filter" in {
        "return a paginated list of all tax rates sorted by name and filtered by updated_since" in new TaxRatesListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val taxRate1 =
            Factory.taxRate(merchant, name = Some("A taxRate"), overrideNow = Some(now.minusDays(1))).create
          val taxRate2 = Factory.taxRate(merchant, name = Some("B taxRate"), overrideNow = Some(now)).create
          val taxRate3 =
            Factory.taxRate(merchant, name = Some("C taxRate"), overrideNow = Some(now.plusDays(1))).create

          Get(s"/v1/tax_rates.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val taxRates = responseAs[PaginatedApiResponse[Seq[TaxRate]]]
            taxRates.data.map(_.id) ==== Seq(taxRate2.id, taxRate3.id)
            taxRates.pagination.totalCount ==== 2
          }
        }
      }

      "with location_id filter and expand[]=locations" in {
        "return a paginated list of all tax rates sorted by name and filtered by location" in new TaxRatesListFSpecContext {
          val taxRate1 = Factory.taxRate(merchant, name = Some("A taxRate"), locations = Seq(rome)).create
          val taxRate2 = Factory.taxRate(merchant, name = Some("B taxRate"), locations = Seq(rome, london)).create
          val taxRate3 = Factory.taxRate(merchant, name = Some("C taxRate"), locations = Seq(london)).create

          Get(s"/v1/tax_rates.list?location_id=${rome.id}&expand[]=locations")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val parsedResponse = responseAs[PaginatedApiResponse[Seq[TaxRate]]]
            val taxRates = parsedResponse.data
            assertResponse(taxRates.head, taxRate1, locations = Some(Set(rome)))
            assertResponse(taxRates(1), taxRate2, locations = Some(Set(rome)))
            parsedResponse.pagination.totalCount ==== 2
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new TaxRatesListFSpecContext {
        Get(s"/v1/tax_rates.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
