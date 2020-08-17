package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures

class ExportsCategorySalesListViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsCategorySalesListViewFSpecContext extends ExportsFSpecContext with OrdersFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/category_sales.list" in {
    "if request has valid token" in {

      "create and process an category sales list" in new ExportsCategorySalesListViewFSpecContext {
        Post(s"/v1/exports/category_sales.list?$defaultBaseParams&field[]=gross_sales")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "category_sales")
          }
        }
      }
    }
  }
}
