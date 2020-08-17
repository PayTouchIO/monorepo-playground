package io.paytouch.ordering.clients.paytouch.core

import akka.http.scaladsl.model._
import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.PtClientSpec
import io.paytouch.ordering.utils.UUIDConversion

trait PtCoreClientSpec extends PtClientSpec {

  val coreUri = Uri("http://my.example.com").taggedWith[PtCoreClient]

  def completeUri(path: String): String = s"$coreUri$path"

  abstract class CoreClientSpecContext extends PtCoreClient(coreUri) with PtClientSpecContext with UUIDConversion {
    def assertUserAuthRequest[T: Manifest](
        method: HttpMethod,
        path: String,
        authToken: HttpHeader,
        body: Option[T] = None,
        queryParams: Option[String] = None,
      )(implicit
        request: HttpRequest,
      ) = {
      request.method ==== method
      request.uri.path.toString ==== path
      request.headers.contains(authToken) should beTrue
      request.uri.rawQueryString ==== queryParams
      if (body.isDefined) {
        request.entity.contentType ==== ContentTypes.`application/json`
        request.entity.body === marshalToSnakeCase(body)
        fromJsonToEntity[T](unmarshalToCamelCase(request.entity.body)) ==== body.get
      }
    }
  }

}
