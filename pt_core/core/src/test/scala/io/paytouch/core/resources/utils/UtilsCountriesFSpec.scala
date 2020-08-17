package io.paytouch.core.resources.utils

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities.{ ApiResponse, Country }

class UtilsCountriesFSpec extends UtilsFSpec {
  abstract class UtilsTimeZonesResourceFSpecContext extends UtilResourceFSpecContext

  "GET /v1/utils.countries" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return all the countries" in new UtilsTimeZonesResourceFSpecContext {

          Get(s"/v1/utils.countries").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val countries = responseAs[ApiResponse[Seq[Country]]].data

            countries must not be empty
            countries.exists(_.code == "US") should beTrue
          }
        }
      }
    }
  }
}
