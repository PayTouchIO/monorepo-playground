package io.paytouch.core.resources

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.utils.FSpec

class PingResourceFSpec extends FSpec {

  abstract class PingResourceFSpecContext extends FSpecContext

  "GET /ping" in {
    "return pong" in new PingResourceFSpecContext {

      Get("/ping") ~> routes ~> check {
        assertStatusOK()
      }
    }
  }
}
