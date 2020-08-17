package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.ordertaxrates.OrderTaxRatesFSpecFixtures

class ExportsOrderTaxRateAggrViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrOrderTaxRateViewFSpecContext extends ExportsFSpecContext with OrderTaxRatesFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/order_tax_rates.sum" in {
    "if request has valid token" in {

      "create and process an order tax rates sum" in new ExportsAggrOrderTaxRateViewFSpecContext {
        Post(s"/v1/exports/order_tax_rates.sum?$defaultBaseParams&field[]=amount")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "order_tax_rates")
          }
        }
      }
    }
  }

  "POST /v1/exports/order_tax_rates.average" in {
    "if request has valid token" in {

      "create and process an order tax rates average" in new ExportsAggrOrderTaxRateViewFSpecContext {
        Post(s"/v1/exports/order_tax_rates.average?$defaultBaseParams&field[]=amount")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "order_tax_rates")
          }
        }
      }
    }
  }

  "POST /v1/exports/order_tax_rates.count" in {
    "if request has valid token" in {

      "create and process an order tax rates average" in new ExportsAggrOrderTaxRateViewFSpecContext {
        Post(s"/v1/exports/order_tax_rates.count?$defaultBaseParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "order_tax_rates")
          }
        }
      }
    }
  }
}
