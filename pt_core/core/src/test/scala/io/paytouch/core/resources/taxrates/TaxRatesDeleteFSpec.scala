package io.paytouch.core.resources.taxrates

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TaxRatesDeleteFSpec extends TaxRatesFSpec {

  abstract class TaxRateDeleteResourceFSpecContext extends TaxRatesResourceFSpecContext {
    def assertTaxRateDoesntExist(id: UUID) = taxRateDao.findById(id).await should beNone
    def assertTaxRateExists(id: UUID) = taxRateDao.findById(id).await should beSome
  }

  "POST /v1/tax_rates.delete" in {

    "if request has valid token" in {
      "if tax rate doesn't exist" should {
        "do nothing and return 204" in new TaxRateDeleteResourceFSpecContext {
          val nonExistingTaxRateId = UUID.randomUUID

          Post(s"/v1/tax_rates.delete", Ids(ids = Seq(nonExistingTaxRateId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertTaxRateDoesntExist(nonExistingTaxRateId)
          }
        }
      }

      "if tax rate belongs to the merchant" should {
        "delete the tax rate and return 204" in new TaxRateDeleteResourceFSpecContext {
          val taxRate = Factory.taxRate(merchant).create

          Post(s"/v1/tax_rates.delete", Ids(ids = Seq(taxRate.id))).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertTaxRateDoesntExist(taxRate.id)
          }
        }
      }

      "if tax rate belongs to a different merchant" should {
        "do not delete the tax rate and return 204" in new TaxRateDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorTaxRate = Factory.taxRate(competitor).create

          Post(s"/v1/tax_rates.delete", Ids(ids = Seq(competitorTaxRate.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertTaxRateExists(competitorTaxRate.id)
          }
        }
      }
    }
  }
}
