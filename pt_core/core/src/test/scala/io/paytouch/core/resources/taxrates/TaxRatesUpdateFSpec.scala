package io.paytouch.core.resources.taxrates

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TaxRatesUpdateFSpec extends TaxRatesFSpec {

  abstract class TaxRatesUpdateFSpecContext extends TaxRatesResourceFSpecContext

  "POST /v1/tax_rates.update?tax_rate_id=<tax_rate_id>" in {
    "if request has valid token" in {
      "if tax rate doesn't exist yet" should {
        "return 404" in new TaxRatesUpdateFSpecContext {
          val newTaxRateId = UUID.randomUUID
          val taxRateUpdate = random[TaxRateUpdate]

          Post(s"/v1/tax_rates.update?tax_rate_id=$newTaxRateId", taxRateUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if tax rate already exists" should {
        "update tax rate and return 200" in new TaxRatesUpdateFSpecContext {
          val newYork = Factory.location(merchant).create
          val taxRate = Factory.taxRate(merchant).create
          val taxRateNewYork = Factory.taxRateLocation(taxRate, newYork).create

          val taxRateUpdate = random[TaxRateUpdate].copy(
            locationOverrides = Map(
              rome.id -> Some(TaxRateLocationUpdate(active = Some(true))),
              london.id -> None,
            ),
          )

          Post(s"/v1/tax_rates.update?tax_rate_id=${taxRate.id}", taxRateUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(taxRateUpdate, taxRate.id)
            assertResponseById(responseAs[ApiResponse[TaxRate]].data, taxRate.id)

            assertItemLocationExists(taxRate.id, newYork.id)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new TaxRatesUpdateFSpecContext {
        val taxRateUpdate = random[TaxRateUpdate]

        Post(s"/v1/tax_rates.update?tax_rate_id=${UUID.randomUUID}", taxRateUpdate)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
