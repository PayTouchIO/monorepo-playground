package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures

class ExportsOrderAggrViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrOrderViewFSpecContext extends ExportsFSpecContext with OrdersFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/orders.sum" in {
    "if request has valid token" in {

      "create and process an orders sum" in new ExportsAggrOrderViewFSpecContext {
        Post(s"/v1/exports/orders.sum?$defaultBaseParams&field[]=profit")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "orders")
          }
        }
      }
    }
  }

  "POST /v1/exports/orders.average" in {
    "if request has valid token" in {

      "create and process an orders average" in new ExportsAggrOrderViewFSpecContext {
        Post(s"/v1/exports/orders.average?$defaultBaseParams&field[]=profit")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "orders")
          }
        }
      }
    }
  }

  "POST /v1/exports/orders.count" in {
    "if request has valid token" in {

      "create and process an orders average" in new ExportsAggrOrderViewFSpecContext {
        Post(s"/v1/exports/orders.count?$defaultBaseParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "orders")
          }
        }
      }
    }
  }
}
