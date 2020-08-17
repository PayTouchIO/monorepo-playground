package io.paytouch.core.resources

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.utils._
import org.json4s.JsonAST.JObject

class SwaggerResourceFSpec extends FSpec {

  abstract class SwaggerResourceFSpecContext extends FSpecContext

  "GET /v1/swagger" in {

    "if request provides valid secret" should {
      "return some data" in new SwaggerResourceFSpecContext {
        Get("/v1/swagger?secret=4f188d6538e4362ffe5473be77ceba3e") ~> routes ~> check {
          assertStatusOK()
          responseAs[JObject] should not(beNull)
        }
      }
    }
  }
}
