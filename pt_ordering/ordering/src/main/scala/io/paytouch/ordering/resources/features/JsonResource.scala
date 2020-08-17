package io.paytouch.ordering.resources.features

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.entities._
import io.paytouch.ordering.errors.{ BadRequest, ClientError, Errors, Error => Err }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.json.serializers.CustomUnmarshallers
import io.paytouch.ordering.resources.Authentication
import io.paytouch.ordering.utils.ResultType
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.{ FindResult, ServiceConfigurations, UpsertionResult, ValidationHeaderName }
import io.paytouch.utils.{ RejectionMsg, StrictEntitiesDirectories }

trait JsonResource
    extends Authentication
       with JsonSupport
       with CustomUnmarshallers
       with StrictEntitiesDirectories
       with LazyLogging {

  val responseTimeout = ServiceConfigurations.responseTimeout

  def completeAsPaginatedApiResponse[T](findResult: FindResult[T])(implicit pagination: Pagination): Route =
    extractRequestContext { ctx =>
      val (data, count) = findResult
      val paginationLinks = PaginationLinks(pagination, ctx.request.uri, count)
      complete(PaginatedApiResponse(data, paginationLinks))
    }

  def completeAsOptApiResponse[T <: ExposedEntity](opt: Option[T]): Route =
    opt match {
      case Some(t) => completeAsApiResponse(t)
      case None    => complete(StatusCodes.NotFound, None)
    }

  def completeAsApiResponse[T <: ExposedEntity](t: T): Route =
    complete(ApiResponse(t))

  def completeAsApiResponse[T <: ExposedEntity](upsertionResult: UpsertionResult[T]): Route =
    (createdOrUpdatedResp[T] orElse handleErrors)(upsertionResult)

  def completeAsValidatedApiResponse[T <: ExposedEntity](validatedT: ValidatedData[T]): Route =
    validatedT match {
      case Valid(t) => completeAsApiResponse(t)
      case i        => handleErrors(i)
    }

  def completeAsEmptyResponse(result: ValidatedData[Unit]): Route =
    toStrict {
      (noContentResp orElse handleErrors)(result)
    }

  private def createdOrUpdatedResp[T <: ExposedEntity]: PartialFunction[UpsertionResult[T], Route] = {
    case Valid((ResultType.Created, t)) => complete(StatusCodes.Created, ApiResponse(t))
    case Valid((ResultType.Updated, t)) => complete(StatusCodes.OK, ApiResponse(t))
  }

  private def noContentResp: PartialFunction[ValidatedData[_], Route] = {
    case Valid(_) => complete(StatusCodes.NoContent)
  }

  private def handleErrors: PartialFunction[ValidatedData[_], Route] = {
    case Invalid(i) if i.exists(_.isInstanceOf[ClientError]) =>
      logger.error(s"Unhandled ClientError: $i")
      respondWithInternalServerError()
    case Invalid(i) if i.exists(_.isInstanceOf[BadRequest]) =>
      respondWithHeader(ValidationHeader(i))(complete(StatusCodes.BadRequest, Errors(i)))
    case Invalid(i) => respondWithHeader(ValidationHeader(i))(complete(StatusCodes.NotFound, Errors(i)))
  }

  protected def respondWithInternalServerError() = {
    var code = "InternalServerError"
    respondWithHeader(RawHeader(ValidationHeaderName, code)) {
      complete(StatusCodes.InternalServerError, RejectionMsg(code))
    }
  }

  protected def ValidationHeader(errors: NonEmptyList[Err]): RawHeader = {
    val codes = errors.toList.map(_.code).distinct.mkString(", ")
    RawHeader(ValidationHeaderName, codes)
  }

}
