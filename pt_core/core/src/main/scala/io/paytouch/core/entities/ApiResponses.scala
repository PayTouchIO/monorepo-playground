package io.paytouch.core.entities

import akka.http.scaladsl.server.RequestContext

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.json.JsonSupport.JValue

final case class PaginatedApiResponse[T](
    data: T,
    `object`: String,
    pagination: PaginationLinks,
  )

object PaginatedApiResponse {
  def apply[T](data: Seq[T], pagination: PaginationLinks): PaginatedApiResponse[Seq[T]] =
    PaginatedApiResponse(data, "list", pagination)
}

final case class ApiResponse[T](data: T, `object`: String)

object ApiResponse {
  def apply[T <: ExposedEntity](data: T): ApiResponse[T] =
    ApiResponse(data, data.classShortName.entryName)

  def apply[T](data: Seq[T]): ApiResponse[Seq[T]] =
    ApiResponse(data, "list")
}

final case class ApiResponseWithMetadata[T](
    data: T,
    `object`: String,
    pagination: PaginationLinks,
    meta: ResponseMetadata,
  )

final case class ResponseMetadata(salesSummary: Option[JValue], typeSummary: Option[JValue])

object ApiResponseWithMetadata extends JsonSupport {
  def apply[T, MD <: Metadata[_, _]](
      data: Seq[T],
      metadata: MD,
    )(implicit
      ctx: RequestContext,
      pagination: Pagination,
    ): ApiResponseWithMetadata[Seq[T]] = {
    val paginationLinks = PaginationLinks(pagination, ctx.request.uri, metadata.count)
    val salesSummaryAsJson = metadata.salesSummary.map(ss => JsonSupport.fromEntityToJValue(ss))
    val typeSummaryAsJson = metadata.typeSummary.map(ts => JsonSupport.fromEntityToJValue(ts))
    val responseMetadata = ResponseMetadata(salesSummaryAsJson, typeSummaryAsJson)
    ApiResponseWithMetadata(data, "list", paginationLinks, responseMetadata)
  }
}
