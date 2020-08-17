package io.paytouch.core.clients.urbanairship

import akka.http.scaladsl.model._

import io.paytouch.utils.StrictEntitiesHelper

import scala.concurrent._

final case class Error(
    uri: Uri,
    requestBody: String,
    status: StatusCode,
    responseBody: String,
    ex: Option[Throwable],
  ) {

  private val statusMessage = s"Request to URI: $uri failed with $status."
  private val requestMessage = s"Request body was: $requestBody."
  private val responseMessage = s"Response body was: $responseBody."
  private val exceptionMessage = ex.fold("")(e => s"Exception thrown: ${e.getMessage}.")

  val message = s"[UA] $statusMessage $requestMessage $responseMessage $exceptionMessage"
}

object Error {

  def asUAResponse[T](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
      ex: Option[Throwable] = None,
    ): UAResponse[T] =
    Left(
      Error(
        uri = request.uri,
        requestBody = requestEntity.data.utf8String,
        status = response.status,
        responseBody = responseEntity.data.utf8String,
        ex = ex,
      ),
    )
}
