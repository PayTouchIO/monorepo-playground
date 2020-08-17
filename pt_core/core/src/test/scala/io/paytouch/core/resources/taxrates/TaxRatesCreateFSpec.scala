package io.paytouch.core.resources.taxrates

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.utils.SetupStepsAssertions

class TaxRatesCreateFSpec extends TaxRatesFSpec {

  abstract class TaxRatesCreateFSpecContext extends TaxRatesResourceFSpecContext with SetupStepsAssertions

  "POST /v1/tax_rates.create?tax_rate_id=$" in {
    "if request has valid token" in {
      "if tax rate doesn't exist yet" should {
        "create tax rate and return 201" in new TaxRatesCreateFSpecContext {
          val newTaxRateId = UUID.randomUUID
          val taxRateCreation = random[TaxRateCreation]

          Post(s"/v1/tax_rates.create?tax_rate_id=$newTaxRateId", taxRateCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val taxRateResponse = responseAs[ApiResponse[TaxRate]].data
            assertUpdate(taxRateCreation.asUpdate, taxRateResponse.id)
            assertResponseById(taxRateResponse, taxRateResponse.id)
            assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupTaxes)
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new TaxRatesCreateFSpecContext {
        val newTaxRateId = UUID.randomUUID
        val taxRateCreation = random[TaxRateCreation]

        Post(s"/v1/tax_rates.create?tax_rate_id=$newTaxRateId", taxRateCreation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
