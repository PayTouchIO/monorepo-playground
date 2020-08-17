package io.paytouch.ordering.resources.merchants

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.ordering.utils.{ CommonArbitraries, FixtureDaoFactory => Factory }

class MerchantsValidateUrSlugFSpec extends MerchantsFSpec with CommonArbitraries {

  "GET /v1/merchants.validate_url_slug" in {
    "if request has valid token" in {

      "if url slug is valid" should {
        "return 204" in new MerchantResourceFSpecContext {
          val slug = "a-valid-slug"
          Get(s"/v1/merchants.validate_url_slug?url_slug=$slug")
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
          }

        }
      }

      "if url slug is already taken" should {
        "reject the request" in new MerchantResourceFSpecContext {
          val invalidSlug = competitor.urlSlug
          Get(s"/v1/merchants.validate_url_slug?url_slug=$invalidSlug")
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("UrlSlugAlreadyTaken")
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new MerchantResourceFSpecContext {
        val slug = "mySlug"
        Get(s"/v1/merchants.validate_url_slug?url_slug=$slug")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
