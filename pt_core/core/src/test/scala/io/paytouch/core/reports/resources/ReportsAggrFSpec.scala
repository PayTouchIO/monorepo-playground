package io.paytouch.core.reports.resources

import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import io.paytouch.core.reports.entities.ReportResponse
import io.paytouch.core.reports.views.ReportAggrView

abstract class ReportsAggrFSpec[V <: ReportAggrView] extends ReportsFSpec[V] {

  def view: V

  val fixtures: ReportsAggrFSpecContext

  abstract class ReportsAggrFSpecContext extends ReportsFSpecContext {

    val allFieldsParams = view.fieldsEnum.values.map(_.entryName).mkString(",")

    def assertAllFieldsResult[T](expectedResults: T*)(implicit um: FromResponseUnmarshaller[ReportResponse[T]]) =
      s"with all fields" in {
        assertResponseWithDefaultDates(
          s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}&field[]=${fixtures.allFieldsParams}",
          None,
          expectedResults: _*,
        )
      }

    def assertGroupByResult[T](
        groupBy: String,
        expectedResults: T*,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      s"by $groupBy" in {
        assertResponseWithDefaultDates(
          s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}&field[]=${fixtures.allFieldsParams}&group_by=$groupBy",
          None,
          expectedResults: _*,
        )
      }
  }
}
