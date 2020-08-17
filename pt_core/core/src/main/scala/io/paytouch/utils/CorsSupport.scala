package io.paytouch.utils

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{ HttpHeader, HttpResponse }
import akka.http.scaladsl.server.Directives.{ complete, handleRejections, mapInnerRoute, respondWithHeaders }
import akka.http.scaladsl.server.{ Directive0, MethodRejection, RejectionHandler }
import io.paytouch.utils.Tagging.withTag

trait CorsAllowOrigins
trait AppHeaderName
trait VersionHeaderName

trait CorsSupport {

  val corsAllowOrigins: List[String] withTag CorsAllowOrigins
  val AppHeaderName: String withTag AppHeaderName
  val VersionHeaderName: String withTag VersionHeaderName

  private val corsAllowedHeaders: List[String] = List(
    "Origin",
    "X-Requested-With",
    "Content-Type",
    "Accept",
    "Accept-Encoding",
    "Accept-Language",
    "Host",
    "Referer",
    "User-Agent",
    "Authorization",
    AppHeaderName,
    VersionHeaderName,
  )

  private val corsAllowCredentials: Boolean = true

  private val optionsCorsHeaders: List[HttpHeader] = List[HttpHeader](
    `Access-Control-Allow-Headers`(corsAllowedHeaders.mkString(", ")),
    `Access-Control-Max-Age`(60 * 60 * 24 * 20), // cache pre-flight response for 20 days
    `Access-Control-Allow-Credentials`(corsAllowCredentials),
  )

  protected def corsRejectionHandler(allowOrigin: `Access-Control-Allow-Origin`) =
    RejectionHandler
      .newBuilder()
      .handle {
        case MethodRejection(supported) =>
          complete(
            HttpResponse().withHeaders(
              `Access-Control-Allow-Methods`(OPTIONS, GET, DELETE, PUT, PATCH, POST, supported) ::
                allowOrigin ::
                optionsCorsHeaders,
            ),
          )
      }
      .result()

  private def originToAllowOrigin(origin: Origin): Option[`Access-Control-Allow-Origin`] =
    if (corsAllowOrigins.contains("*") || corsAllowOrigins.contains(origin.value))
      origin.origins.headOption.map(`Access-Control-Allow-Origin`.apply)
    else
      None

  def cors[T]: Directive0 =
    mapInnerRoute { route => context =>
      ((context.request.method, context.request.header[Origin].flatMap(originToAllowOrigin)) match {
        case (OPTIONS, Some(allowOrigin)) =>
          handleRejections(corsRejectionHandler(allowOrigin)) {
            respondWithHeaders(allowOrigin, `Access-Control-Allow-Credentials`(corsAllowCredentials)) {
              route
            }
          }
        case (_, Some(allowOrigin)) =>
          respondWithHeaders(allowOrigin, `Access-Control-Allow-Credentials`(corsAllowCredentials)) {
            route
          }
        case (_, _) =>
          route
      })(context)
    }
}
