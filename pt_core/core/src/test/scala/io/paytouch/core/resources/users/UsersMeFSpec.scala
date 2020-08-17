package io.paytouch.core.resources.users

import cats.implicits._

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.enums.{ BusinessType, ImageUploadType }
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ MerchantSetupStatus, MerchantSetupSteps }
import io.paytouch.core.entities.enums.MerchantSetupStatus._
import io.paytouch.core.entities.enums.MerchantSetupSteps._
import io.paytouch.core.services.UtilService
import io.paytouch.core.utils.{ DisabledUserFixtures, UtcTime, FixtureDaoFactory => Factory }

class UsersMeFSpec extends UsersFSpec {
  abstract class UsersMeFSpecContext extends UserResourceFSpecContext {
    // override to isOwner=false so user can be disabled/deleted
    override lazy val user = Factory
      .user(
        merchant,
        firstName = Some(firstName),
        lastName = Some(lastName),
        password = Some(password),
        email = Some(email),
        locations = locations,
        userRole = Some(userRole),
        isOwner = Some(false),
      )
      .create

  }

  "GET /v1/users.me" in {
    "if request has valid token" in {

      "if user has been disabled" should {

        "reject the request" in new UsersMeFSpecContext {
          Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }

          val itemUpdate = UpdateActiveItem(itemId = user.id, active = false)
          userDao.bulkUpdateActiveField(merchantId = merchant.id, updates = Seq(itemUpdate)).await

          Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
            rejection should beAnInstanceOf[AuthenticationFailedRejection]
          }
        }
      }

      "if user has been deleted" should {

        "reject the request" in new UsersMeFSpecContext {

          Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }

          val itemUpdate = UpdateActiveItem(itemId = user.id, active = false)
          userDao.deleteByIdsAndMerchantId(ids = Seq(user.id), merchantId = merchant.id).await

          Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
            rejection should beAnInstanceOf[AuthenticationFailedRejection]
          }
        }
      }
      "with no parameters" should {
        "return the user information without a password and with permissions" in new UsersMeFSpecContext {

          Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(userEntity, user, withPermissions = true)
          }
        }

        "return the user information for a user with no extra data" in new UsersMeFSpecContext {
          val noDataUser = Factory.user(merchant).create
          val noDataAuthorizationHeader = {
            val jwtToken = Factory.createValidTokenWithSession(noDataUser)

            Authorization(OAuth2BearerToken(jwtToken))
          }

          Get("/v1/users.me").addHeader(noDataAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(userEntity, noDataUser, withPermissions = true)
          }
        }

        "return user with image uploads" in new UsersMeFSpecContext {
          val imageUpload1 = Factory
            .imageUpload(merchant, Some(user.id), Some(Map("a" -> "1", "b" -> "2")), Some(ImageUploadType.User))
            .create
          val imageUpload2 = Factory
            .imageUpload(merchant, Some(user.id), Some(Map("c" -> "3", "d" -> "4")), Some(ImageUploadType.User))
            .create

          Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val userEntity = responseAs[ApiResponse[User]].data

            assertResponse(userEntity, user, withPermissions = true, imageUploads = Seq(imageUpload1, imageUpload2))
          }
        }
      }

      "with expand[]=locations" should {
        "return the list of locations the user belongs to" in new UsersMeFSpecContext {

          Get("/v1/users.me?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(userEntity, user, withPermissions = true)
            userEntity.locations.map(_.map(_.id)).get should containTheSameElementsAs(Seq(london.id, rome.id))
          }
        }
      }

      "with expand[]=merchant" should {
        "return the merchant the user belongs to" in new UsersMeFSpecContext {
          val setupSteps =
            Map[MerchantSetupSteps, MerchantSetupStep](MerchantSetupSteps.DesignReceipts -> MerchantSetupStep())
          merchantDao.updateSetupSteps(merchant.id, setupCompleted = false, updatedSteps = setupSteps).await
          val reloadedMerchant = merchantDao.findById(merchant.id).await.get

          Get("/v1/users.me?expand[]=merchant").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(userEntity, user, withPermissions = true, merchant = Some(reloadedMerchant))
          }
        }
      }

      "with expand[]=merchant,merchant_setup_steps" should {
        "return the merchant the user belongs to with expanded setup steps (restaurant)" in new UsersMeFSpecContext {
          override lazy val merchant = Factory.merchant(businessType = Some(BusinessType.Restaurant)).create

          val setupSteps =
            Map[MerchantSetupSteps, MerchantSetupStep](
              DesignReceipts -> MerchantSetupStep(),
              ImportCustomers -> MerchantSetupStep(skippedAt = Some(UtcTime.now)),
              ImportProducts -> MerchantSetupStep(completedAt = Some(UtcTime.now)),
            )

          merchantDao.updateSetupSteps(merchant.id, setupCompleted = false, updatedSteps = setupSteps).await
          val reloadedMerchant = merchantDao.findById(merchant.id).await.get

          Get("/v1/users.me?expand[]=merchant,merchant_setup_steps").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(
              userEntity,
              user,
              withPermissions = true,
              merchant = Some(reloadedMerchant),
              withMerchantSetupSteps = true,
            )
          }
        }

        "return the merchant the user belongs to with expanded setup steps (retail)" in new UsersMeFSpecContext {
          override lazy val merchant = Factory.merchant(businessType = Some(BusinessType.Retail)).create

          val setupSteps =
            Map[MerchantSetupSteps, MerchantSetupStep](
              DesignReceipts -> MerchantSetupStep(),
              ImportCustomers -> MerchantSetupStep(skippedAt = Some(UtcTime.now)),
              ImportProducts -> MerchantSetupStep(completedAt = Some(UtcTime.now)),
            )
          merchantDao.updateSetupSteps(merchant.id, setupCompleted = false, updatedSteps = setupSteps).await
          val reloadedMerchant = merchantDao.findById(merchant.id).await.get

          Get("/v1/users.me?expand[]=merchant,merchant_setup_steps").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(
              userEntity,
              user,
              withPermissions = true,
              merchant = Some(reloadedMerchant),
              withMerchantSetupSteps = true,
            )
          }
        }
      }

      "with expand[]=merchant,merchant_legal_details" should {
        "return the merchant the user belongs to with expanded legal details (address)" in new UsersMeFSpecContext {
          override lazy val merchant =
            Factory
              .merchant(
                businessType = BusinessType.Retail.some,
                legalDetails = LegalDetailsUpsertion
                  .empty
                  .copy(
                    address = AddressImprovedUpsertion
                      .empty
                      .copy(
                        countryCode = "US".some,
                        stateCode = "CA".some,
                      )
                      .some,
                  )
                  .some,
              )
              .create

          val expectedLegalDetails =
            LegalDetails
              .empty
              .copy(
                address = AddressImproved
                  .empty
                  .copy(
                    countryData = UtilService.Geo.UnitedStates.some,
                    stateData = AddressState(
                      name = "California".some,
                      code = "CA",
                      UtilService.Geo.UnitedStates.some,
                    ).some,
                  )
                  .some,
              )

          val reloadedMerchant = merchantDao.findById(merchant.id).await.get

          Get("/v1/users.me?expand[]=merchant,merchant_legal_details").addHeader(
            authorizationHeader,
          ) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(
              userEntity,
              user,
              withPermissions = true,
              merchant = reloadedMerchant.some,
              merchantLegalDetails = expectedLegalDetails.some,
            )
          }
        }
      }

      "with expand[]=access" should {
        "return the info on user access" in new UsersMeFSpecContext {
          Get("/v1/users.me?expand[]=access").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val userEntity = responseAs[ApiResponse[User]].data
            assertResponse(userEntity, user, withPermissions = true, access = Some(buildAccess(userRole)))
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new UsersMeFSpecContext {

        Get(s"/v1/users.me").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }

    "if the user associated to the token is disabled" should {

      "be rejected" in new UsersMeFSpecContext with DisabledUserFixtures {

        Get(s"/v1/users.me").addHeader(disabledAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
