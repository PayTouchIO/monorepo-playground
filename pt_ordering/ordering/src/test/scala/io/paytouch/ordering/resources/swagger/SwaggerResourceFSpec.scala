package io.paytouch.ordering.resources.swagger

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.ordering.utils.FSpec
import org.json4s.JsonAST.JObject

class SwaggerResourceFSpec extends FSpec {

  abstract class SwaggerResourceFSpecContext extends FSpecContext

  "GET /v1/swagger" in {

    "if request provides valid secret" should {
      "return some data" in new SwaggerResourceFSpecContext {
        Get("/v1/swagger?secret=dYLpW9vaMBA7ETyAagPnhJv3NU8CIxs2") ~> routes ~> check {
          assertStatusOK()
          responseAs[JObject] should not(beNull)
        }
      }
    }
  }
}
