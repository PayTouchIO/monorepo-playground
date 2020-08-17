package io.paytouch.ordering.resources

import io.paytouch.ordering.utils.FSpec

class PingResourceFSpec extends FSpec {
  "GET /ping" should {
    "return pong" in new FSpecContext {
      Get("/ping") ~> routes ~> check {
        assertStatusOK()
      }
    }
  }
}
