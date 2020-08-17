package io.paytouch.core.resources.merchants

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch._

import io.paytouch.core.entities._
import io.paytouch.core.errors.InvalidAddress
import io.paytouch.core.services.UtilService

class MerchantsUpdateFSpec extends MerchantsFSpec {
  abstract class MerchantsUpdateFSpecContext extends MerchantResourceFSpecContext {
    val merchantDao = daos.merchantDao

    def assertUpdate(
        update: ApiMerchantUpdate,
        id: UUID,
        legalDetails: Option[LegalDetails] = None,
      ) = {
      val record = merchantDao.findById(id).await.get
      id ==== record.id
      if (update.businessName.isDefined) update.businessName ==== Some(record.businessName)
      if (update.zoneId.isDefined) update.zoneId ==== Some(record.defaultZoneId)

      legalDetails.foreach { details =>
        record.legalDetails.foreach(_ ==== details)
        record.legalCountry ==== details.country
      }
    }

    final def assertGoodLegalDetails(
        inputCountryCode: Option[String],
        inputStateCode: Option[String],
        expectedCountry: Option[Country],
        expectedState: Option[AddressState],
      ): Unit =
      assertLegalDetails(inputCountryCode, inputStateCode) { (update, merchantId) =>
        assertStatusOK()

        responseAs[ApiResponse[Merchant]].data

        assertUpdate(
          update,
          merchantId,
          legalDetails = LegalDetails
            .empty
            .copy(
              address = AddressImproved
                .empty
                .copy(
                  countryData = expectedCountry,
                  stateData = expectedState,
                )
                .some,
            )
            .some,
        )
      }

    final def assertBadLegalDetails(
        inputCountryCode: Option[String],
        inputStateCode: Option[String],
        error: InvalidAddress,
      ): Unit =
      assertLegalDetails(inputCountryCode, inputStateCode) { (_, _) =>
        assertStatus(StatusCodes.BadRequest)
        assertErrorMessage(error.message)
      }

    private def assertLegalDetails[T](
        inputCountryCode: Option[String],
        inputStateCode: Option[String],
      )(
        body: (ApiMerchantUpdate, UUID) => T,
      ): Unit = {
      val update = random[ApiMerchantUpdate].copy(
        legalDetails = LegalDetailsUpsertion
          .empty
          .copy(
            address = AddressImprovedUpsertion
              .empty
              .copy(
                countryCode = inputCountryCode,
                stateCode = inputStateCode,
              )
              .some,
          )
          .some,
      )

      Post(s"/v1/merchants.update?merchant_id=${merchant.id}", update)
        .addHeader(authorizationHeader) ~> sealedRoutes ~> check(body(update, merchant.id))
    }
  }

  "POST /v1/merchants.update?merchant_id=$" in {
    "if request has valid token" in {
      "email is new" should {
        "update merchant and return 201" in new MerchantsUpdateFSpecContext {
          val update = random[ApiMerchantUpdate]

          Post(s"/v1/merchants.update?merchant_id=${merchant.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val merchant = responseAs[ApiResponse[Merchant]].data
            assertUpdate(update, merchant.id)
          }
        }
      }

      "with legal details specified" should {
        "update a merchant with legal details" should {
          "find the country and state" in new MerchantsUpdateFSpecContext {
            val country = UtilService.Geo.UnitedStates

            assertGoodLegalDetails(
              inputCountryCode = country.code.some,
              inputStateCode = "CA".some,
              expectedCountry = country.some,
              expectedState = AddressState(name = "California".some, code = "CA", country.some).some,
            )
          }

          "find the country but not the state" in new MerchantsUpdateFSpecContext {
            assertBadLegalDetails(
              inputCountryCode = UtilService.Geo.UnitedStates.code.some,
              inputStateCode = None,
              error = InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether,
            )
          }

          "find the country but not the state for countries with known states" in new MerchantsUpdateFSpecContext {
            UtilService.Geo.countriesWithSupportedStates.foreach { country =>
              assertBadLegalDetails(
                inputCountryCode = country.code.some,
                inputStateCode = "does not exist".some,
                error = InvalidAddress.InvalidState(StateCode("does not exist")),
              )
            }
          }

          "find the country but not the state name" in new MerchantsUpdateFSpecContext {
            val country = Country(name = "Italy", code = "IT")

            assertGoodLegalDetails(
              inputCountryCode = country.code.some,
              inputStateCode = "does not exist".some,
              expectedCountry = country.some,
              expectedState = AddressState(name = None, code = "does not exist", country.some).some,
            )
          }

          "neither find the country nor the state 1" in new MerchantsUpdateFSpecContext {
            assertBadLegalDetails(
              inputCountryCode = "does not exist".some,
              inputStateCode = "CA".some,
              error = InvalidAddress.InvalidCountry(CountryCode("does not exist")),
            )
          }

          "neither find the country nor the state 2" in new MerchantsUpdateFSpecContext {
            assertBadLegalDetails(
              inputCountryCode = None,
              inputStateCode = "CA".some,
              error = InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether,
            )
          }

          "neither find the country nor the state 3" in new MerchantsUpdateFSpecContext {
            assertGoodLegalDetails(
              inputCountryCode = None,
              inputStateCode = None,
              expectedCountry = None,
              expectedState = None,
            )
          }
        }
      }

      "merchant does not exists" should {
        "return 404" in new MerchantsUpdateFSpecContext {
          val merchantId = UUID.randomUUID
          val update = random[ApiMerchantUpdate]

          Post(s"/v1/merchants.update?merchant_id=$merchantId", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new MerchantsUpdateFSpecContext {
        val merchantId = UUID.randomUUID
        val update = random[ApiMerchantUpdate]
        Post(s"/v1/merchants.update?merchant_id=$merchantId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
