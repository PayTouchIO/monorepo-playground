package io.paytouch.ordering.utils

import akka.http.scaladsl.model.headers.{ `WWW-Authenticate`, Connection }
import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server._
import io.paytouch.logging.LoggedException
import io.paytouch.ordering.logging.HttpLogging
import io.paytouch.ordering.resources.features.JsonResource
import io.paytouch.utils.RejectionMsg
import sangria.parser.{ SyntaxError => GraphQLSyntaxError }

trait CustomHandlers extends JsonResource { self: HttpLogging =>

  private val pathPrefixes = "v1" | "graphql"

  lazy val rejectNonMatchedRoutes: Route =
    pathPrefix((!pathPrefixes)()) {
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
      .handleAll[Rejection] { rejections =>
        val status =
          if (rejections.exists(_.isInstanceOf[ValidationRejection])) StatusCodes.NotFound
          else StatusCodes.BadRequest
        complete(status, RejectionMsg(rejections.mkString(",")))
      }
      .result()

  implicit def customExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case ex: GraphQLSyntaxError =>
        handleException(status = StatusCodes.BadRequest, ex.formattedError, ex)
      case ex: Throwable =>
        val msg = "Something went wrong. Highly trained monkeys are looking into it."
        handleException(status = StatusCodes.InternalServerError, msg, ex)
    }

  private def handleException(
      status: StatusCode,
      msg: String,
      ex: Throwable,
    ) =
    toStrict {
      extractRequest { request =>
        mdcActor ! LoggedException(request, ex, UtcTime.now)
        complete(status, RejectionMsg(msg))
      }
    }

}
