package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.groups.GroupsFSpecFixtures

class ExportsGroupTopViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrGroupViewFSpecContext extends ExportsFSpecContext with GroupsFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/groups.top" in {
    "if request has valid token" in {

      "create and process an groups top" in new ExportsAggrGroupViewFSpecContext {
        Post(s"/v1/exports/groups.top?$defaultBaseParams&order_by[]=id")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "groups")
          }
        }
      }
    }
  }
}
