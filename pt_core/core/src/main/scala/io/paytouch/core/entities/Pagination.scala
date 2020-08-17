package io.paytouch.core.entities

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query

final case class Pagination(page: Int, perPage: Int) {
  require(1 <= page, "page numbering starts from 1")
  require(perPage <= 100, "maximum 100 items per page")

  val limit = perPage
  val offset = perPage * (page - 1)
}

final case class PaginationLinks(
    prev: Option[String],
    next: Option[String],
    first: String,
    last: String,
    perPage: Int,
    totalCount: Int,
  )

object PaginationLinks {

  def apply(
      pagination: Pagination,
      baseUri: Uri,
      count: Int,
    ): PaginationLinks = {
    def genUrl(query: Map[String, String], pg: Int): String =
      baseUri.withQuery(Query(query + ("page" -> pg.toString))).toString

    val perPage = pagination.perPage
    val currentPage = baseUri.query().getOrElse("page", "1").toInt

    val firstPage = 1
    val prevPage = List(currentPage - 1, firstPage).max
    val pageCount = List(math.ceil(count.toDouble / perPage).toInt, firstPage).max

    val pageLimit = baseUri.query().get("page_limit").map(_.toInt).getOrElse(pageCount)
    val lastPage = List(pageLimit, pageCount).min
    val nextPage = List(currentPage + 1, lastPage).min
    val totalCount = if (lastPage < pageCount) lastPage * perPage else count

    val originalQueryMap: Map[String, String] = baseUri.query().toMap
    val prevUrl = if (firstPage != currentPage) Some(genUrl(originalQueryMap, prevPage)) else None
    val nextUrl = if (lastPage != currentPage) Some(genUrl(originalQueryMap, nextPage)) else None
    val firstUrl = genUrl(originalQueryMap, firstPage)
    val lastUrl = genUrl(originalQueryMap, lastPage)

    PaginationLinks(
      prev = prevUrl,
      next = nextUrl,
      first = firstUrl,
      last = lastUrl,
      perPage = perPage,
      totalCount = totalCount,
    )
  }
}
