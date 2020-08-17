package io.paytouch.core.resources.utils

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ ApiResponse, TimeZone => TimeZoneEntity }

class UtilsTimeZonesFSpec extends UtilsFSpec {

  abstract class UtilsTimeZonesResourceFSpecContext extends UtilResourceFSpecContext

  "GET /v1/utils.time_zones" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return all the time zones" in new UtilsTimeZonesResourceFSpecContext {

          Get(s"/v1/utils.time_zones").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val timeZones = responseAs[ApiResponse[Seq[TimeZoneEntity]]].data

            timeZones must not be empty
            timeZones.exists(_.id contains "Rome") should beTrue
          }
        }
      }
    }
  }
}
