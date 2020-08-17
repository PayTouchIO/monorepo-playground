package io.paytouch.core.processors

import scala.concurrent._

import io.paytouch.core.entities._
import io.paytouch.core.messages.entities.OrderSynced
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.MultipleLocationFixtures

class OrderSyncedProcessorSpec extends ProcessorSpec {
  abstract class OrderSyncedProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    implicit val u: UserContext = userContext

    @scala.annotation.nowarn("msg=Auto-application")
    val order = random[Order]
    val adminReportServiceMock = mock[AdminReportService]

    lazy val processor = new OrderSyncedProcessor(adminReportServiceMock)
  }

  "OrderSyncedProcessor" in {

    "trigger compute reports for the order" in new OrderSyncedProcessorSpecContext {
      adminReportServiceMock.triggerUpdateReports(any) returns Future.successful(5)

      processor.execute(OrderSynced(order))

      val filters = AdminReportFilters(ids = Some(Seq(order.id)))

      afterAWhile {
        there was one(adminReportServiceMock).triggerUpdateReports(filters)
      }
    }
  }
}
