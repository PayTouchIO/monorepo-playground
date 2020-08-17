package io.paytouch.core.resources.utils

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ ApiResponse, State }

class UtilsStatesFSpec extends UtilsFSpec {

  abstract class UtilsTimeZonesResourceFSpecContext extends UtilResourceFSpecContext

  "GET /v1/utils.states" in {
    "if request has valid token" in {
      "with country_code = us" should {
        "return all the us states" in new UtilsTimeZonesResourceFSpecContext {

          Get(s"/v1/utils.states?country_code=us").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val states = responseAs[ApiResponse[Seq[State]]].data

            states.size ==== 55
            states.exists(s => s.code == "CA" && s.name == "California") should beTrue
          }
        }
      }

      "with country_code = ca" should {
        "return all the ca states" in new UtilsTimeZonesResourceFSpecContext {

          Get(s"/v1/utils.states?country_code=ca").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val states = responseAs[ApiResponse[Seq[State]]].data

            states.size ==== 13
            states.exists(s => s.code == "QC" && s.name == "Quebec") should beTrue
          }
        }
      }

      "with a non-supported country_code" should {
        "return 200 and empty list" in new UtilsTimeZonesResourceFSpecContext {

          Get(s"/v1/utils.states?country_code=my-code").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val states = responseAs[ApiResponse[Seq[State]]].data
            states ==== Seq.empty
          }
        }
      }
    }
  }
}
