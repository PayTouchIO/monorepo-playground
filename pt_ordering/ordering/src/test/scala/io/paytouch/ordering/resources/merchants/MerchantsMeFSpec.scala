package io.paytouch.ordering.resources.merchants

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.ordering.entities._

class MerchantsMeFSpec extends MerchantsFSpec {

  "GET /v1/merchants.me" in {
    "if request has valid token" in {
      "return the merchant" in new MerchantResourceFSpecContext {
        Get("/v1/merchants.me").addHeader(userAuthorizationHeader) ~> routes ~> check {
          status === StatusCodes.OK

          val entity = responseAs[ApiResponse[Merchant]].data
          assertResponse(entity, merchant)
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new MerchantResourceFSpecContext {
        Get("/v1/merchants.me")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
