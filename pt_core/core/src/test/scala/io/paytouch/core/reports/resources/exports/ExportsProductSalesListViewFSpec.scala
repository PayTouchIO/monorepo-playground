package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures

class ExportsProductSalesListViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsProductSalesListViewFSpecContext extends ExportsFSpecContext with OrdersFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/product_sales.list" in {
    "if request has valid token" in {

      "create and process an product sales list" in new ExportsProductSalesListViewFSpecContext {
        Post(s"/v1/exports/product_sales.list?$defaultBaseParams&field[]=returned_quantity")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "product_sales")
          }
        }
      }
    }
  }
}
