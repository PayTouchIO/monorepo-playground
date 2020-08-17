package io.paytouch.core.resources.oauth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class OauthAuthorizeFSpec extends OauthFSpec {

  abstract class OauthAuthorizeFSpecContext extends OauthResourceFSpecContext

  "POST /v1/oauth.authorize" in {
    "if request has valid token" should {
      "if redirect uri is correct" should {
        // https://dashboard.paytouch.io/oauth/authorize?response_type=code&client_id=5455b450-7589-40d0-b9ce-ccf722d818cb&scope=all&state=123123123123&redirect_uri=https://paytouch-dot-ipaas-on-gae.appspot.com/api/v1/oauth/redirect
        "generate a code and return to client" in new OauthAuthorizeFSpecContext {
          Post(
            s"/v1/oauth.authorize?response_type=code&client_id=${oauthApp.clientId}&redirect_uri=$validRedirectUri&scope=all",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val oauthCodeRecord = oauthCodeDao.findAllByMerchantId(merchant.id).await.head
            val oauthCode = responseAs[ApiResponse[OauthCode]].data
            oauthCodeRecord.code ==== oauthCode.code
            oauthCodeRecord.userId ==== user.id
            oauthCodeRecord.oauthAppId ==== oauthApp.id
          }
        }
      }

      "if redirect uri is invalid" should {
        "return 400 with OauthInvalidRedirectUri" in new OauthAuthorizeFSpecContext {
          Post(
            s"/v1/oauth.authorize?response_type=code&client_id=${oauthApp.clientId}&redirect_uri=$invalidRedirectUri&scope=all&state=1234zyx",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("OauthInvalidRedirectUri")
          }
        }
      }

      "if client id is invalid" should {
        "return 404 with InvalidOauthClientIds" in new OauthAuthorizeFSpecContext {
          Post(
            s"/v1/oauth.authorize?response_type=code&client_id=$generateUuid&redirect_uri=$invalidRedirectUri&scope=all&state=1234zyx",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("InvalidOauthClientIds")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new OauthAuthorizeFSpecContext {
        Post(
          s"/v1/oauth.authorize?response_type=code&client_id=$generateUuid&redirect_uri=$invalidRedirectUri&scope=all&state=1234zyx",
        ).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
