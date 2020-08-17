package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.locationgiftcardpasses.LocationGiftCardPassesFSpecFixtures

class ExportsLocationGiftCardPassAggrViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsLocationGiftCardPassListViewFSpecContext
      extends ExportsFSpecContext
         with LocationGiftCardPassesFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/location_gift_card_passes.sum" in {
    "if request has valid token" in {

      "create and process an location sales sum" in new ExportsLocationGiftCardPassListViewFSpecContext {
        Post(s"/v1/exports/location_gift_card_passes.sum?$defaultBaseParams&field[]=customers")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "location_gift_card_passes")
          }
        }
      }
    }
  }
}
