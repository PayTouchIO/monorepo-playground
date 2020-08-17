package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.resources.giftcardpasses.GiftCardPassesFSpecFixtures

class ExportsGiftCardPassAggrViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsAggrCustomerViewFSpecContext extends ExportsFSpecContext with GiftCardPassesFSpecFixtures {
    val exportDao = daos.exportDao
  }

  "POST /v1/exports/gift_card_passes.sum" in {
    "if request has valid token" in {

      "create and process an gift card passes sum" in new ExportsAggrCustomerViewFSpecContext {
        Post(s"/v1/exports/gift_card_passes.sum?$defaultBaseParams&field[]=customers")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "gift_card_passes")
          }
        }
      }
    }
  }

  "POST /v1/exports/gift_card_passes.average" in {
    "if request has valid token" in {

      "create and process an gift card passes average" in new ExportsAggrCustomerViewFSpecContext {
        Post(s"/v1/exports/gift_card_passes.average?$defaultBaseParams&field[]=customers")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "gift_card_passes")
          }
        }
      }
    }
  }

  "POST /v1/exports/gift_card_passes.count" in {
    "if request has valid token" in {

      "create and process an gift card passes average" in new ExportsAggrCustomerViewFSpecContext {
        Post(s"/v1/exports/gift_card_passes.count?$defaultBaseParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val export = responseAs[ApiResponse[Export]].data
          afterAWhile {
            val record = exportDao.findById(export.id).await.get
            record.status ==== ExportStatus.Completed
            assertBaseUrl(record, "gift_card_passes")
          }
        }
      }
    }
  }
}
