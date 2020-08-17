package io.paytouch.core.reports.resources.exports

import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures

class ExportsEmployeeSalesListViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsemployeeSalesListViewFSpecContext extends ExportsFSpecContext with OrdersFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/employee_sales.list" in {
    "if request has valid token" in {

      "create and process an employee sales list" in new ExportsemployeeSalesListViewFSpecContext {
        Post(s"/v1/exports/employee_sales.list?$defaultBaseParams&field[]=net_sales")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "employee_sales")
          }
        }
      }
    }
  }
}
