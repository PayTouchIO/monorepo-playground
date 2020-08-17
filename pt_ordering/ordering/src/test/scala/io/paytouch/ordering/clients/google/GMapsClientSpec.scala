package io.paytouch.ordering.clients.google

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Referer
import io.paytouch.ordering._

trait GMapsClientSpec extends GClientSpec {

  val key = "my-google-key".taggedWith[GMapsClient]

  abstract class GDirectionClientSpecContext extends GMapsClient(key) with GClientSpecContext {

    def assertKeyRequest[T: Manifest](
        method: HttpMethod,
        path: String,
        queryParams: Option[String] = None,
      )(implicit
        request: HttpRequest,
      ) = {
      request.method ==== method
      request.uri.path.toString ==== path
      request.headers.contains(Referer(referer)) should beTrue
      val actualRawQuery = request.uri.rawQueryString.getOrElse("")
      actualRawQuery.contains(s"key=$key") should beTrue
      actualRawQuery ==== (queryParams.toList ++ List(s"key=$key")).mkString("&")
    }
  }

}
