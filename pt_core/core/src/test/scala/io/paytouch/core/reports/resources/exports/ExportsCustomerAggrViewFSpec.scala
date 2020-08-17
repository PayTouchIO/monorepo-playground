package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.customers.CustomerFSpecFixtures

class ExportsCustomerAggrViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrCustomerViewFSpecContext extends ExportsFSpecContext with CustomerFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/customers.sum" in {
    "if request has valid token" in {

      "create and process an customers sum" in new ExportsAggrCustomerViewFSpecContext {
        Post(s"/v1/exports/customers.sum?$defaultBaseParams&field[]=spend")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "customers")
          }
        }
      }
    }
  }

  "POST /v1/exports/customers.average" in {
    "if request has valid token" in {

      "create and process an customers average" in new ExportsAggrCustomerViewFSpecContext {
        Post(s"/v1/exports/customers.average?$defaultBaseParams&field[]=spend")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "customers")
          }
        }
      }
    }
  }

  "POST /v1/exports/customers.count" in {
    "if request has valid token" in {

      "create and process an customers average" in new ExportsAggrCustomerViewFSpecContext {
        Post(s"/v1/exports/customers.count?$defaultBaseParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "customers")
          }
        }
      }
    }
  }
}
