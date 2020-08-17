package io.paytouch.core.resources.auth0

import java.util.UUID
import java.time.ZoneId

import cats.implicits._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.resources.admin.merchants.{ MerchantsFSpec => AdminMerchantsFSpec }
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }
import io.paytouch.core.USD

class Auth0RegistrationFSpec extends AdminMerchantsFSpec {
  abstract class Auth0RegistrationFSpecContext extends MerchantResourceFSpecContext with Auth0Fixtures {
    lazy val token = generateAuth0JwtToken()

    val merchantService = MockedRestApi.merchantService
    val auth0Client = MockedRestApi.auth0Client
    lazy val email = auth0Client.emailForUserId(subject)

    def assertCreation(creation: Auth0Registration, merchantId: UUID) = {
      val merchantRecord = daos.merchantDao.findById(merchantId).await.get
      merchantRecord.mode ==== creation.mode
      merchantRecord.setupType ==== creation.setupType

      val userRecord = daos.userDao.findOneByMerchantId(merchantId).await.get
      userRecord.active ==== true
      userRecord.isOwner ==== true
      userRecord.auth0UserId ==== Some(subject)
      userRecord.email ==== email
    }

    def assertLoadingSuccessful(merchantId: UUID) = {
      val merchant = merchantDao.findById(merchantId).await.get
      merchant.loadingStatus ==== LoadingStatus.Successful
    }
  }

  "POST /v1/auth0.registration" in {
    "if request has valid token" in {
      "email is new" should {
        "create demo merchant and return 201" in new Auth0RegistrationFSpecContext {
          val creation = randomAuth0Registration().copy(token = token, mode = MerchantMode.Demo)
          Post("/v1/auth0.registration", creation) ~> routes ~> check {
            assertStatusCreated()

            val merchant = responseAs[ApiResponse[Merchant]].data
            assertCreation(creation, merchant.id)

            afterAWhile {
              assertLoadingSuccessful(merchant.id)
            }
          }
        }

        "create production merchant and return 201" in new Auth0RegistrationFSpecContext {
          val creation = randomAuth0Registration().copy(token = token, mode = MerchantMode.Production)
          Post("/v1/auth0.registration", creation) ~> routes ~> check {
            assertStatusCreated()

            val merchant = responseAs[ApiResponse[Merchant]].data
            assertCreation(creation, merchant.id)

            afterAWhile {
              assertLoadingSuccessful(merchant.id)
            }
          }
        }

        "create a merchant with dummy data and return 201" in new Auth0RegistrationFSpecContext {
          val creation = Auth0Registration(
            token = token,
            businessType = BusinessType.Restaurant,
            businessName = "My Restaurant",
            address = AddressUpsertion(
              line1 = "101 Restaurant Rd".some,
              line2 = None,
              city = "Bakers Town".some,
              state = "CA".some,
              stateCode = "CA".some,
              country = "US".some,
              countryCode = "US".some,
              postalCode = "90210".some,
            ),
            restaurantType = RestaurantType.FastCasual,
            mode = MerchantMode.Production,
            setupType = SetupType.Dash,
            currency = USD,
            zoneId = ZoneId.of("UTC"),
            pin = None,
            dummyData = true,
          )

          Post("/v1/auth0.registration", creation) ~> routes ~> check {
            assertStatusCreated()

            val merchant = responseAs[ApiResponse[Merchant]].data
            assertCreation(creation, merchant.id)

            val merchantRecord = daos.merchantDao.findById(merchant.id).await.get
            merchantRecord.businessName ==== "My Restaurant"
            merchantRecord.businessType ==== BusinessType.Restaurant
            merchantRecord.restaurantType ==== RestaurantType.FastCasual
            merchantRecord.setupType ==== SetupType.Dash

            val locationRecords = daos.locationDao.findAllByMerchantId(merchant.id).await
            locationRecords.length ==== 1
            val locationRecord = locationRecords.head
            locationRecord.name ==== "My Restaurant"
            locationRecord.email ==== email.some
            locationRecord.addressLine1 ==== "101 Restaurant Rd".some
            locationRecord.addressLine2 ==== None
            locationRecord.city ==== "Bakers Town".some
            locationRecord.state ==== "California".some
            locationRecord.stateCode ==== "CA".some
            locationRecord.country ==== "United States of America".some
            locationRecord.countryCode ==== "US".some
            locationRecord.postalCode ==== "90210".some
            locationRecord.active ==== true
            locationRecord.dummyData ==== true

            afterAWhile {
              assertLoadingSuccessful(merchant.id)
            }
          }
        }
      }

      "owner user email already exists" should {
        "return 400" in new Auth0RegistrationFSpecContext {

          val merchant = Factory.merchant.create
          val user = Factory.user(merchant, email = Some(email)).create

          val creation = randomAuth0Registration().copy(token = token)
          Post("/v1/auth0.registration", creation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("EmailAlreadyInUse")
          }
        }
      }

      "owner user auth0 id already exists" should {
        "return 400" in new Auth0RegistrationFSpecContext {
          val merchant = Factory.merchant.create
          val user = Factory.user(merchant, auth0UserId = Some(subject)).create

          val creation = randomAuth0Registration().copy(token = token)
          Post("/v1/auth0.registration", creation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("UserAlreadyExists")
          }
        }
      }

      "token is invalid" should {
        "return an error" in new Auth0RegistrationFSpecContext {
          val creation = randomAuth0Registration().copy(token = "not-a-valid-token")
          Post("/v1/auth0.registration", creation) ~> routes ~> check {
            assertStatus(StatusCodes.Unauthorized)
            assertErrorCodesAtLeastOnce("UnauthorizedError")
          }
        }
      }
    }
  }
}
