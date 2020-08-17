package io.paytouch.ordering.resources

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.ordering.utils.FSpec

class UnknownResourceFSpec extends FSpec {
  "For an unknown request" should {
    // default behaviour is 400
    "reply with a 404" in new FSpecContext {
      Get("/whatever/whatever/whatever") ~> routes ~> check {
        assertStatus(StatusCodes.NotFound)
      }
    }
  }
}
