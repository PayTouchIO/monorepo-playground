package io.paytouch.core.services

import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.UtcTime

class AdminReportServiceSpec extends ServiceDaoSpec {
  abstract class AdminReportServiceSpecContext extends ServiceDaoSpecContext with OrdersFSpecFixtures {
    val service = new AdminReportService(db)
    val emptyFilter = new AdminReportFilters(None)
  }

  "AdminReportService" should {
    "triggerUpdateReports for some orders" in new AdminReportServiceSpecContext {
      val affectedRows = service
        .triggerUpdateReports(emptyFilter.copy(ids = Some(Seq(order1.id))))
        .await
      affectedRows ==== 1
    }

    "triggerUpdateReports for some merchants" in new AdminReportServiceSpecContext {
      val affectedRows = service
        .triggerUpdateReports(emptyFilter.copy(merchantIds = Some(Seq(merchant.id))))
        .await
      affectedRows ==== 6
    }

    "triggerUpdateReports for some locations" in new AdminReportServiceSpecContext {
      val affectedRows = service
        .triggerUpdateReports(emptyFilter.copy(locationIds = Some(Seq(rome.id))))
        .await
      affectedRows ==== 5
    }

    "triggerUpdateReports from a given date and merchant" in new AdminReportServiceSpecContext {
      val today = UtcTime.now
      val affectedRows = service
        .triggerUpdateReports(emptyFilter.copy(from = Some(today.minusYears(2)), merchantIds = Some(Seq(merchant.id))))
        .await
      affectedRows ==== 6
    }

    "triggerUpdateReports to a given date and merchant" in new AdminReportServiceSpecContext {
      val today = UtcTime.now
      val affectedRows = service
        .triggerUpdateReports(emptyFilter.copy(to = Some(today.plusYears(2)), merchantIds = Some(Seq(merchant.id))))
        .await

      println(s"[flaky][AdminReportServiceSpec debugging] $affectedRows")

      affectedRows ==== 6
    }
  }
}
