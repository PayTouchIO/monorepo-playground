package io.paytouch.core.resources.pusher

import java.util.UUID

import akka.http.scaladsl.model._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FSpec, MultipleLocationFixtures }

class PusherAuthFSpec extends FSpec {

  abstract class PusherAuthFSpecContext extends FSpecContext with MultipleLocationFixtures

  "POST /v1/pusher.auth" in {

    "authenticate a user if credentials are valid" in new PusherAuthFSpecContext {
      val channel = s"private-${user.merchantId}-${rome.id}-channel"
      val postParams = Map("channel_name" -> channel, "socket_id" -> "123.45")
      val requestEntity = FormData(postParams).toEntity

      Post("/v1/pusher.auth", requestEntity).addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()
        responseAs[PusherToken] should not beNull
      }
    }

    "reject pusher authentication if merchant id is invalid" in new PusherAuthFSpecContext {
      val channel = s"private-${UUID.randomUUID}-${UUID.randomUUID}-channel"
      val postParams = Map("channel_name" -> channel, "socket_id" -> "123.45")
      val requestEntity = FormData(postParams).toEntity

      Post("/v1/pusher.auth", requestEntity).addHeader(authorizationHeader) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }
  }
}
