package io.paytouch.ordering.clients.paytouch

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import io.paytouch.ordering.clients.{ ClientSpec, PaytouchClient }

trait PtClientSpec extends ClientSpec {

  trait PtClientSpecContext extends PaytouchClient with ClientSpecContext {
    implicit val authToken = Authorization(OAuth2BearerToken("a-valid-token"))
  }

}
