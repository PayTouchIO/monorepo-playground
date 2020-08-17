package io.paytouch.core.resources.admin.merchants

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch._

import io.paytouch.core.data.model.enums.SetupType
import io.paytouch.core.entities._
import io.paytouch.core.errors.InvalidAddress
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class MerchantsCreateFSpec extends MerchantsFSpec {
  abstract class MerchantsCreationFSpecContext extends MerchantResourceFSpecContext {
    final def assertGoodLegalDetails(
        inputCountryCode: Option[String],
        inputStateCode: Option[String],
        expectedCountry: Option[Country],
        expectedState: Option[AddressState],
      ): Unit =
      assertLegalDetails(inputCountryCode, inputStateCode) { (creation, merchantId) =>
        assertStatusCreated()

        responseAs[ApiResponse[Merchant]].data

        assertCreation(
          creation,
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
        body: (MerchantCreation, UUID) => T,
      ): Unit = {
      val merchantId = UUID.randomUUID

      @scala.annotation.nowarn("msg=Auto-application")
      val creation = random[MerchantCreation].copy(
        email = randomEmail,
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

      Post(s"/v1/admin/merchants.create?merchant_id=$merchantId", creation)
        .addHeader(adminAuthorizationHeader) ~> sealedRoutes ~> check(body(creation, merchantId))
    }
  }

  "POST /v1/admin/merchants.create?merchant_id=$" in {
    "if request has valid token" in {
      "email is new" should {
        "create merchant and return 201" in new MerchantsCreationFSpecContext {
          val merchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[MerchantCreation].copy(email = randomEmail)

          Post(s"/v1/admin/merchants.create?merchant_id=$merchantId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[Merchant]].data

            assertCreation(creation, merchantId)
          }
        }
      }

      "with no features provided" should {
        "create a merchant with default features" in new MerchantsCreationFSpecContext {
          val merchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[MerchantCreation].copy(email = randomEmail, features = None)

          Post(s"/v1/admin/merchants.create?merchant_id=$merchantId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[Merchant]].data

            assertCreation(creation, merchantId, Some(creation.setupType.features))
          }
        }
      }

      "with features specified" should {
        "create a merchant with features" in new MerchantsCreationFSpecContext {
          val merchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[MerchantCreation].copy(
              email = randomEmail,
              setupType = SetupType.Dash,
              features = Some(
                MerchantFeaturesUpsertion(
                  pos = Some(MerchantFeature(enabled = true)),
                ),
              ),
            )

          Post(s"/v1/admin/merchants.create?merchant_id=$merchantId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[Merchant]].data

            assertCreation(
              creation,
              merchantId,
              Some(SetupType.Dash.features.copy(pos = MerchantFeature(enabled = true))),
            )
          }
        }
      }

      "with legal details specified" should {
        "create a merchant with legal details" should {
          "find the country and state" in new MerchantsCreationFSpecContext {
            val coutry = UtilService.Geo.UnitedStates

            assertGoodLegalDetails(
              inputCountryCode = coutry.code.some,
              inputStateCode = "CA".some,
              expectedCountry = coutry.some,
              expectedState = AddressState(name = "California".some, code = "CA", coutry.some).some,
            )
          }

          "find the country but not the state" in new MerchantsCreationFSpecContext {
            assertBadLegalDetails(
              inputCountryCode = UtilService.Geo.UnitedStates.code.some,
              inputStateCode = None,
              error = InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether,
            )
          }

          "find the country but not the state for countries with known states" in new MerchantsCreationFSpecContext {
            UtilService.Geo.countriesWithSupportedStates.foreach { country =>
              assertBadLegalDetails(
                inputCountryCode = country.code.some,
                inputStateCode = "does not exist".some,
                error = InvalidAddress.InvalidState(StateCode("does not exist")),
              )
            }
          }

          "find the country but not the state name" in new MerchantsCreationFSpecContext {
            val country = Country(name = "Italy", code = "IT")

            assertGoodLegalDetails(
              inputCountryCode = country.code.some,
              inputStateCode = "does not exist".some,
              expectedCountry = country.some,
              expectedState = AddressState(name = None, code = "does not exist", country.some).some,
            )
          }

          "neither find the country nor the state 1" in new MerchantsCreationFSpecContext {
            assertBadLegalDetails(
              inputCountryCode = "does not exist".some,
              inputStateCode = "CA".some,
              error = InvalidAddress.InvalidCountry(CountryCode("does not exist")),
            )
          }

          "neither find the country nor the state 2" in new MerchantsCreationFSpecContext {
            assertBadLegalDetails(
              inputCountryCode = None,
              inputStateCode = "CA".some,
              error = InvalidAddress.CountryAndStateCodesMustBeSubmittedTogether,
            )
          }

          "neither find the country nor the state 3" in new MerchantsCreationFSpecContext {
            assertGoodLegalDetails(
              inputCountryCode = None,
              inputStateCode = None,
              expectedCountry = None,
              expectedState = None,
            )
          }
        }
      }

      "merchant id already exists" should {
        "return 400" in new MerchantsCreationFSpecContext {
          val merchant = Factory.merchant.create

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[MerchantCreation]

          Post(s"/v1/admin/merchants.create?merchant_id=${merchant.id}", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "owner user email already exists" should {
        "return 400" in new MerchantsCreationFSpecContext {
          val merchant = Factory.merchant.create
          val user = Factory.user(merchant).create

          val newMerchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[MerchantCreation].copy(email = user.email)

          Post(s"/v1/admin/merchants.create?merchant_id=$newMerchantId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("EmailAlreadyInUse")
          }
        }
      }

      "owner user email already exists" should {
        "return 400" in new MerchantsCreationFSpecContext {
          val newMerchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[MerchantCreation].copy(email = "yadda")

          Post(s"/v1/admin/merchants.create?merchant_id=$newMerchantId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new MerchantsCreationFSpecContext {
        val merchantId = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[MerchantCreation]
        Post(s"/v1/admin/merchants.create?merchant_id=$merchantId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
