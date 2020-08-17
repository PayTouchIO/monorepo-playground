package io.paytouch.ordering
package clients

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, RawHeader }
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.clients.paytouch.core.CoreErrorResponse
import io.paytouch.ordering.entities.{ ApiResponse, PaginatedApiResponse }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.logging.HttpLogging
import io.paytouch.utils.{ HttpClient, StrictEntitiesHelper }

import scala.concurrent.Future

trait PaytouchClient extends HttpClient with JsonSupport with StrictEntitiesHelper with LazyLogging with HttpLogging {

  type Wrapper[T] = PaytouchResponse[T]

  def uri: Uri

  protected val httpClient = {
    val scheme = uri.scheme.toLowerCase
    val host = uri.authority.host.toString
    val port = uri.effectivePort
    if (scheme == "https") Http().outgoingConnectionHttps(host = host, port = port)
    else Http().outgoingConnection(host = host, port = port)
  }

  private val appName = RawHeader(AppHeaderName, "pt_ordering")
  private val versionApp = RawHeader(VersionHeaderName, "N/A")

  def sendAndReceiveAsApiResponse[T: Manifest](request: HttpRequest): Future[CoreApiResponse[T]] =
    sendAndReceive[ApiResponse[T]](request)

  def sendAndReceiveAsPaginatedApiResponse[T: Manifest](request: HttpRequest): Future[CoreApiResponse[Seq[T]]] = {

    type CorePaginatedApiResponse[T] = PaytouchResponse[PaginatedApiResponse[Seq[T]]]

    def extractData(coreResponse: CoreApiResponse[Seq[T]]): Seq[T] =
      coreResponse match {
        case Right(coreData) => coreData.data
        case Left(error) =>
          logger.error(s"Request went bad when doing pagination. request -> ${request.uri}, error -> $error")
          Seq.empty
      }

    def extractNextPage(coreResponse: CorePaginatedApiResponse[T]): Option[String] =
      coreResponse.toOption.flatMap(_.pagination.next)

    def triggerPagination(nextPage: Option[String]): Future[Seq[T]] =
      nextPage match {
        case None       => Future.successful(Seq.empty)
        case Some(page) => sendAndReceiveAsPaginatedApiResponse[T](request.copy(uri = Uri(page))).map(extractData)
      }

    def mergeData(response: CorePaginatedApiResponse[T], remainingData: Seq[T]): CoreApiResponse[Seq[T]] =
      response.map { paginatedResp =>
        val currentData = paginatedResp.data
        ApiResponse(data = currentData ++ remainingData, `object` = paginatedResp.`object`)
      }

    for {
      response <- sendAndReceive[PaginatedApiResponse[Seq[T]]](request)
      nextPage = extractNextPage(response)
      remainingData <- triggerPagination(nextPage)
    } yield mergeData(response, remainingData)
  }

  def sendAndReceiveEmpty(request: HttpRequest): Future[PaytouchResponse[Unit]] =
    super.sendAndReceiveEmpty(request, (_, _, _, _) => Future.successful(Right(())))

  protected def prepareRequest(request: HttpRequest): HttpRequest = request.withDefaultHeaders(appName, versionApp)

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]] =
    jsonUnmarshaller[T].apply(responseEntity.withContentType(MediaTypes.`application/json`)).map(Right(_)).recoverWith {
      case ex: Throwable =>
        logger.error("While parsing response from Paytouch", ex)
        Future.successful(Left(errors.ClientError(request.uri, ex.getMessage)))
    }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ) =
    jsonUnmarshaller[CoreErrorResponse]
      .apply(response.entity)
      .map(response => Left(errors.ClientError(request.uri, response)))

  final implicit class RichHttpRequest(val req: HttpRequest) {
    def withUserAuth(implicit authToken: Authorization) = req.addHeader(authToken)
  }
}
