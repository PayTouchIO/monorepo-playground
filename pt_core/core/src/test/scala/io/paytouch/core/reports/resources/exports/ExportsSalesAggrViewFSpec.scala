package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures

class ExportsSalesAggrViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrSaleViewFSpecContext extends ExportsFSpecContext with OrdersFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/sales.sum" in {
    "if request has valid token" in {

      "create and process an sales sum" in new ExportsAggrSaleViewFSpecContext {
        Post(s"/v1/exports/sales.sum?$defaultBaseParams&field[]=net_sales")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "sales")
          }
        }
      }
    }
  }

  "POST /v1/exports/sales.average" in {
    "if request has valid token" in {

      "create and process an sales average" in new ExportsAggrSaleViewFSpecContext {
        Post(s"/v1/exports/sales.average?$defaultBaseParams&field[]=net_sales")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "sales")
          }
        }
      }
    }
  }

  "POST /v1/exports/sales.count" in {
    "if request has valid token" in {

      "create and process an sales average" in new ExportsAggrSaleViewFSpecContext {
        Post(s"/v1/exports/sales.count?$defaultBaseParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "sales")
          }
        }
      }
    }
  }
}
