package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.products.ProductsFSpecFixtures

class ExportsProductTopViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrProductViewFSpecContext extends ExportsFSpecContext with ProductsFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/products.top" in {
    "if request has valid token" in {

      "create and process an products top" in new ExportsAggrProductViewFSpecContext {
        Post(s"/v1/exports/products.top?$defaultBaseParams&order_by[]=id")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "products")
          }
        }
      }
    }
  }
}
