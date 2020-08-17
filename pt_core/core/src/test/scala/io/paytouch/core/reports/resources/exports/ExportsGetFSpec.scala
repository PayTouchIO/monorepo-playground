package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ExportsGetFSpec extends FSpec {

  abstract class ExportResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val exportDao = daos.exportDao

    def assertResponse(record: ExportRecord, entity: Export) = {
      record.id ==== entity.id
      record.`type` ==== entity.`type`
      record.status ==== entity.status
      record.createdAt ==== entity.createdAt
      record.updatedAt ==== entity.updatedAt
    }
  }

  "GET /v1/exports.get?export_id=$" in {
    "if request has valid token" in {

      "if the export belongs to the merchant" should {
        "return a export" in new ExportResourceFSpecContext {
          val export = Factory.export(merchant).create

          Get(s"/v1/exports.get?export_id=${export.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val exportEntity = responseAs[ApiResponse[Export]].data
            assertResponse(export, exportEntity)
          }
        }
      }

      "if the export does not belong to the merchant" should {
        "return 404" in new ExportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorExport = Factory.export(competitor).create

          Get(s"/v1/exports.get?export_id=${competitorExport.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
