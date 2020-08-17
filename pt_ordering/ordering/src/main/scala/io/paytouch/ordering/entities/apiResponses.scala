package io.paytouch.ordering.entities

final case class ApiResponse[T](data: T, `object`: String)

object ApiResponse {
  def apply[T <: ExposedEntity](data: T): ApiResponse[T] =
    apply[T](data, data.classShortName.entryName)
}

final case class PaginatedApiResponse[T](
    data: T,
    `object`: String,
    pagination: PaginationLinks,
  )

object PaginatedApiResponse {
  def apply[T](data: Seq[T], pagination: PaginationLinks): PaginatedApiResponse[Seq[T]] =
    PaginatedApiResponse(data, "list", pagination)
}
