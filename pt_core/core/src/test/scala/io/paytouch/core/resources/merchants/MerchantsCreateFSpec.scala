package io.paytouch.core.resources.merchants

import cats.implicits._
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthorizationFailedRejection

import io.paytouch.core.data.model.enums.LoadingStatus
import io.paytouch.core.data.model.enums.MerchantMode._
import io.paytouch.core.entities._
import io.paytouch.core.resources.admin.merchants.{ MerchantsFSpec => AdminMerchantsFSpec }
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class MerchantsCreateFSpec extends AdminMerchantsFSpec {
  abstract class MerchantsCreationFSpecContext extends MerchantResourceFSpecContext {
    val merchantService = MockedRestApi.merchantService
    val newMerchantId = UUID.randomUUID

    def assertPublicCreation(creation: PublicMerchantCreation, merchantId: UUID) =
      assertCreation(creation.toMerchantCreation, merchantId)

    def assertLoadingSuccessful(merchantId: UUID) = {
      val merchant = merchantDao.findById(merchantId).await.get
      merchant.loadingStatus ==== LoadingStatus.Successful
    }
  }

  "POST /v1/merchants.create?merchant_id=$" in {
    "if request has valid token" in {
      "email is new" should {
        "create demo merchant and return 201" in new MerchantsCreationFSpecContext {
          val creation = random[PublicMerchantCreation].copy(email = randomEmail, mode = Demo)
          val path = merchantService.generatePath(newMerchantId)

          Post(path, creation) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[Merchant]].data
            assertPublicCreation(creation, newMerchantId)

            afterAWhile {
              assertLoadingSuccessful(newMerchantId)
            }
          }
        }

        "create product merchant and return 201" in new MerchantsCreationFSpecContext {
          val creation = random[PublicMerchantCreation].copy(email = randomEmail, mode = Production)
          val path = merchantService.generatePath(newMerchantId)

          Post(path, creation) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[Merchant]].data
            assertPublicCreation(creation, newMerchantId)

            afterAWhile {
              assertLoadingSuccessful(newMerchantId)
            }
          }
        }

        "create a merchant with dummy data and return 201" in new MerchantsCreationFSpecContext {
          val email = randomEmail

          val creation = random[PublicMerchantCreation].copy(
            email = email,
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
            mode = Production,
            dummyData = true,
          )
          val path = merchantService.generatePath(newMerchantId)

          Post(path, creation) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[Merchant]].data
            assertPublicCreation(creation, newMerchantId)

            val locationRecords = daos.locationDao.findAllByMerchantId(newMerchantId).await
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
            assertAvailabilityUpsertion(locationRecord.id, Availabilities.TwentyFourSeven)

            afterAWhile {
              assertLoadingSuccessful(newMerchantId)
            }
          }
        }
      }

      "merchant id already exists" should {
        "return 400" in new MerchantsCreationFSpecContext {
          val merchant = Factory.merchant.create
          val creation = random[PublicMerchantCreation]
          val path = merchantService.generatePath(merchant.id)

          Post(path, creation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "owner user email already exists" should {
        "return 400" in new MerchantsCreationFSpecContext {
          val merchant = Factory.merchant.create
          val user = Factory.user(merchant).create

          val creation = random[PublicMerchantCreation].copy(email = user.email)
          val path = merchantService.generatePath(newMerchantId)

          Post(path, creation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("EmailAlreadyInUse")
          }
        }
      }

      "owner user email is invalid" should {
        "return 400" in new MerchantsCreationFSpecContext {
          val creation = random[PublicMerchantCreation].copy(email = "yadda")
          val path = merchantService.generatePath(newMerchantId)

          Post(path, creation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }
    }

    "if request has invalid token in URL" should {
      "be rejected" in new MerchantsCreationFSpecContext {
        val creation = random[PublicMerchantCreation]
        Post(s"/v1/merchants.create?merchant_id=$newMerchantId", creation) ~> routes ~> check {
          rejection ==== AuthorizationFailedRejection
        }
      }
    }
  }

}
