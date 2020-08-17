package io.paytouch.core.clients.paytouch.ordering

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import io.paytouch.core.{ PtOrderingPassword, PtOrderingUser }
import io.paytouch.core.clients.paytouch.PtClientSpec
import io.paytouch.core.data.daos.DaoSpec
import io.paytouch.utils.Tagging._

abstract class PtOrderingClientSpec extends DaoSpec with PtClientSpec {

  val ptOrderingUri = uri.taggedWith[PtOrderingClient]
  val ptOrderingUser = user.taggedWith[PtOrderingUser]
  val ptOrderingPassword = pass.taggedWith[PtOrderingPassword]

  def completeUri(path: String): String = s"$ptOrderingUri$path"

  abstract class PtOrderingClientSpecContext
      extends PtOrderingClient(ptOrderingUri, ptOrderingUser, ptOrderingPassword)
         with PtClientSpecContext {

    implicit def toUUID(s: String): UUID = UUID.fromString(s)

    def assertAppAuthRequest[T](
        method: HttpMethod,
        path: String,
        body: Option[T] = None,
      )(implicit
        request: HttpRequest,
      ) = {
      val authToken = Authorization(BasicHttpCredentials(ptOrderingUser, ptOrderingPassword))
      request.method ==== method
      request.uri.path.toString ==== path
      request.headers.contains(authToken) should beTrue
      if (body.isDefined) {
        request.entity.contentType ==== ContentTypes.`application/json`
        request.entity.body === marshalToSnakeCase(body)
      }
    }
  }

}
