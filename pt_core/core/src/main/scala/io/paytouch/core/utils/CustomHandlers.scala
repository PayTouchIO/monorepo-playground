package io.paytouch.core.utils

import scala.util._

import org.json4s.MappingException

import akka.http.scaladsl.model.headers.{ `WWW-Authenticate`, Connection }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.logging.HttpLogging
import io.paytouch.core.resources.JsonResource
import io.paytouch.logging.LoggedException
import io.paytouch.utils.{ RejectionMsg, StrictEntitiesDirectories }

trait CustomHandlers extends LazyLogging with StrictEntitiesDirectories {
  self: JsonResource with HttpLogging =>

  lazy val rejectNonMatchedRoutes: Route =
    pathPrefix((!"v1")() ~ (!"v2")()) {
      extractDataBytes { _ =>
        respondWithHeader(Connection("close"))
        complete(StatusCodes.NotFound)
      }
    }

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      // copied and adapted from akka-http_2.12-10.0.6-sources.jar!/akka/http/scaladsl/server/RejectionHandler.scala
      .handleAll[AuthenticationFailedRejection] { rejections =>
        val rejectionMessage = rejections.head.cause match {
          case CredentialsMissing  => "The resource requires authentication, which was not supplied with the request"
          case CredentialsRejected => "The supplied authentication is invalid"
        }
        // Multiple challenges per WWW-Authenticate header are allowed per spec,
        // however, it seems many browsers will ignore all challenges but the first.
        // Therefore, multiple WWW-Authenticate headers are rendered, instead.
        //
        // See https://code.google.com/p/chromium/issues/detail?id=103220
        // and https://bugzilla.mozilla.org/show_bug.cgi?id=669675
        val authenticateHeaders = rejections.map(r => `WWW-Authenticate`(r.challenge))
        respondWithHeaders(authenticateHeaders) {
          complete(StatusCodes.Unauthorized, RejectionMsg(rejectionMessage))
        }
      }
      .handleAll[LockingFailedRejection] { rejections =>
        complete(StatusCodes.ServiceUnavailable, RejectionMsg(rejections.mkString(",")))
      }
      .handleAll[Rejection](rejections => complete(StatusCodes.BadRequest, RejectionMsg(rejections.mkString(","))))
      .result()

  implicit def customExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case me: MappingException =>
        complete(StatusCodes.BadRequest, RejectionMsg(me.msg))

      case ex: Throwable =>
        Try {
          extractRequest { request =>
            mdcActor ! LoggedException(request, ex, UtcTime.now)
            internalErrorRoute
          }
        } match {
          case Success(route) => route
          case Failure(ex) =>
            logger.error("Something went terribly wrong in the customExceptionHandler", ex)
            internalErrorRoute
        }
    }

  private def internalErrorRoute =
    complete(
      StatusCodes.InternalServerError,
      RejectionMsg("Something went wrong. Highly trained monkeys are looking into it."),
    )

}
