package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.customers.CustomerFSpecFixtures

class ExportsCustomerTopViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsCustomerTopViewFSpecContext extends ExportsFSpecContext with CustomerFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/customers.top" in {
    "if request has valid token" in {

      "create and process an customers top" in new ExportsCustomerTopViewFSpecContext {
        Post(s"/v1/exports/customers.top?$defaultBaseParams&order_by[]=id")
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
