package io.paytouch.core.resources.merchants

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.data.model.enums.MerchantMode
import io.paytouch.core.entities.{ ApiResponse, JsonWebToken, LoginResponse }
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

class MerchantsSwitchModeToFSpec extends MerchantsFSpec {

  abstract class MerchantsSwitchModeToFSpecContext extends MerchantResourceFSpecContext {
    val merchantDao = daos.merchantDao
    val userDao = daos.userDao

    def assertSessionIsNotValid(jwtToken: JsonWebToken) = getUserContext(jwtToken) must beEmpty
    def assertSessionIsValid(jwtToken: JsonWebToken) = getUserContext(jwtToken) must beSome
    def getUserContext(jwtToken: JsonWebToken) = MockedRestApi.authenticationService.getUserContext(jwtToken).await

    def assertUserValuesAreCopied(oldUser: UserRecord, newUserRecordId: UUID) = {
      val newUser = userDao.findById(newUserRecordId).await.get
      oldUser.firstName ==== newUser.firstName
      oldUser.lastName ==== newUser.lastName
      oldUser.encryptedPassword ==== newUser.encryptedPassword
      oldUser.pin ==== newUser.pin
    }

    def assertMerchantIsInProduction(merchantId: UUID) =
      merchantDao.findById(merchantId).await.get.mode ==== MerchantMode.Production
  }

  "POST /v1/merchants.switch_mode_to?mode=$" in {
    "if request has valid token" in {
      "merchant id is the current merchant's id" should {
        "if current merchant is in mode=demo" should {
          "if there is no matching merchant with mode=production" should {
            "create new merchant, migrate existing owner data, invalidate existing tokens and return a new jwtToken" in new MerchantsSwitchModeToFSpecContext {
              override lazy val merchant = Factory.merchant(mode = Some(MerchantMode.Demo)).create
              Post(s"/v1/merchants.switch_mode_to?mode=production")
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                val newJwtToken = JsonWebToken(responseAs[ApiResponse[LoginResponse]].data.value)
                assertSessionIsNotValid(jwtToken)
                assertSessionIsValid(newJwtToken)

                val newUserContext = getUserContext(newJwtToken).get
                newUserContext.id != user.id
                newUserContext.merchantId != merchant.id

                assertMerchantIsInProduction(newUserContext.merchantId)
                assertUserValuesAreCopied(user, newUserContext.id)
              }
            }
          }

          "if there is a matching merchant with mode=production" should {
            "invalidate current session and return a new jwtToken for that merchant" in new MerchantsSwitchModeToFSpecContext {
              override lazy val merchant = Factory.merchant(mode = Some(MerchantMode.Demo)).create
              val productionMerchant = Factory.merchant(mode = Some(MerchantMode.Production)).create
              merchantDao.linkSwitchMerchants(merchant.id, productionMerchant.id).await

              val productionUserRole = Factory.userRole(productionMerchant).create
              val productionUser =
                Factory.user(productionMerchant, userRole = Some(productionUserRole), isOwner = Some(true)).create

              Post(s"/v1/merchants.switch_mode_to?mode=production")
                .addHeader(authorizationHeader) ~> routes ~> check {
                assertStatusOK()
                val newJwtToken = JsonWebToken(responseAs[ApiResponse[LoginResponse]].data.value)
                assertSessionIsNotValid(jwtToken)
                assertSessionIsValid(newJwtToken)

                val newUserContext = getUserContext(newJwtToken).get
                newUserContext.id ==== productionUser.id
                newUserContext.merchantId ==== productionMerchant.id

                assertMerchantIsInProduction(newUserContext.merchantId)
              }
            }
          }
        }

        "if current merchant is in mode=production" should {
          "return 400" in new MerchantsSwitchModeToFSpecContext {
            Post(s"/v1/merchants.switch_mode_to?mode=production")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
            }
          }
        }
      }

      "if request has invalid token" should {

        "be rejected" in new MerchantsSwitchModeToFSpecContext {
          Post(s"/v1/merchants.switch_mode_to?mode=production")
            .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
            rejection should beAnInstanceOf[AuthenticationFailedRejection]
          }
        }
      }
    }
  }
}
