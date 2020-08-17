package io.paytouch.core.reports.resources

import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import io.paytouch.core.entities.{ Pagination, PaginationLinks }
import io.paytouch.core.reports.entities.ReportResponse
import io.paytouch.core.reports.views.ReportListView

abstract class ReportsListFSpec[V <: ReportListView] extends ReportsFSpec[V] {

  def view: V

  val action = "list"

  val fixtures: ReportsListFSpecContext

  abstract class ReportsListFSpecContext extends ReportsFSpecContext {

    val allFieldsParams = view.orderByEnum.values.map(_.entryName).mkString(",")

    def assertFieldResultWithPagination[T](
        field: String,
        totalCount: Int,
        expectedResults: Seq[T],
        pagination: Option[Pagination] = None,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      s"with field $field" in {
        val url = s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}&field[]=$field"
        val paginationLinks = buildPaginationLinks(url, totalCount, pagination)
        assertResponseWithDefaultDates(url, Some(paginationLinks), expectedResults: _*)
      }

    def assertAllFieldsResultWithPagination[T](
        totalCount: Int,
        expectedResults: Seq[T],
        pagination: Option[Pagination] = None,
        extraFilters: Option[String] = None,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      s"with all fields" in {
        val extraParams = extraFilters.fold("")(s => s"&$s") + paginationParams(pagination)
        val url =
          s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}&field[]=${fixtures.allFieldsParams}$extraParams"
        val paginationLinks = buildPaginationLinks(url, totalCount, pagination)
        assertResponseWithDefaultDates(url, Some(paginationLinks), expectedResults: _*)
      }

    def assertOrderByResultWithPagination[T](
        orderBy: String,
        totalCount: Int,
        expectedResults: Seq[T],
        pagination: Option[Pagination] = None,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      s"by $orderBy" in {
        val extraParams = paginationParams(pagination)
        val url =
          s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}&field[]=${fixtures.allFieldsParams}&order_by[]=$orderBy$extraParams"
        val paginationLinks = buildPaginationLinks(url, totalCount, pagination)
        assertResponseWithDefaultDates(url, Some(paginationLinks), expectedResults: _*)
      }

    private def paginationParams(pagination: Option[Pagination]): String =
      pagination.fold("")(p => s"&page=${p.page}&per_page=${p.perPage}")

    private def buildPaginationLinks(
        url: String,
        totalCount: Int,
        pagination: Option[Pagination],
      ): PaginationLinks = {
      val baseUrl = "http://example.com"
      val defaultPagination = Pagination(page = 1, perPage = 30)
      PaginationLinks(pagination.getOrElse(defaultPagination), baseUrl + url, totalCount)
    }
  }
}
