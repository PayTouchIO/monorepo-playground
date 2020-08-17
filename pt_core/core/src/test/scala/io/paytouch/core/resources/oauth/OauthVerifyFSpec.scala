package io.paytouch.core.resources.oauth

import akka.http.scaladsl.model.{ FormData, StatusCodes }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class OauthVerifyFSpec extends OauthFSpec {

  abstract class OauthVerifyFSpecContext extends OauthResourceFSpecContext {
    val oauthCode = Factory.oauthCode(merchant, user, oauthApp).create

    def validateVerificationSuccess(loginResponse: LoginResponse) = {
      val responseJwt = responseAs[ApiResponse[LoginResponse]].data.valueToJwt

      val sessionRecord = sessionDao.findOneByJti(responseJwt.jti.get).await.get

      val oauthAppSessionRecords = oauthAppSessionDao.findAllByMerchantId(merchant.id).await
      oauthAppSessionRecords must haveSize(1)
      val oauthAppSessionRecord = oauthAppSessionRecords.head
      oauthAppSessionRecord.oauthAppId ==== oauthApp.id
      oauthAppSessionRecord.sessionId ==== sessionRecord.id
    }
  }

  "POST /v1/oauth.verify" in {
    "if all parameters are valid" in {
      "receive data via query string generate a session and return a jwt token to user" in new OauthVerifyFSpecContext {
        Post(
          s"/v1/oauth.verify?grant_type=authorization_code&code=${oauthCode.code}&redirect_uri=REDIRECT_URI&client_id=${oauthApp.clientId}&client_secret=${oauthApp.clientSecret}",
        ) ~> routes ~> check {
          assertStatusOK()
          validateVerificationSuccess(responseAs[ApiResponse[LoginResponse]].data)
        }
      }
      "receive data via form fields generate a session and return a jwt token to user" in new OauthVerifyFSpecContext {
        Post(
          s"/v1/oauth.verify",
          FormData(
            "client_id" -> oauthApp.clientId.toString,
            "client_secret" -> oauthApp.clientSecret.toString,
            "code" -> oauthCode.code.toString,
          ).toEntity,
        ) ~> routes ~> check {
          assertStatusOK()
          validateVerificationSuccess(responseAs[ApiResponse[LoginResponse]].data)
        }
      }
    }
    "if code is invalid" in {
      "return 400 and error OauthInvalidCode" in new OauthVerifyFSpecContext {
        Post(
          s"/v1/oauth.verify?grant_type=authorization_code&code=$generateUuid&redirect_uri=REDIRECT_URI&client_id=${oauthApp.clientId}&client_secret=${oauthApp.clientSecret}",
        ) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("OauthInvalidCode")
        }
      }
    }
    "if client id is invalid" in {
      "return 404 and error InvalidOauthClientIds" in new OauthVerifyFSpecContext {
        Post(
          s"/v1/oauth.verify?grant_type=authorization_code&code=${oauthCode.code}&redirect_uri=REDIRECT_URI&client_id=$generateUuid&client_secret=${oauthApp.clientSecret}",
        ) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
          assertErrorCode("InvalidOauthClientIds")
        }
      }
    }
    "if client secret is invalid" in {
      "return 400 and error OauthInvalidClientSecret" in new OauthVerifyFSpecContext {
        Post(
          s"/v1/oauth.verify?grant_type=authorization_code&code=${oauthCode.code}&redirect_uri=REDIRECT_URI&client_id=${oauthApp.clientId}&client_secret=$generateUuid",
        ) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("OauthInvalidClientSecret")
        }
      }
    }
  }

}
