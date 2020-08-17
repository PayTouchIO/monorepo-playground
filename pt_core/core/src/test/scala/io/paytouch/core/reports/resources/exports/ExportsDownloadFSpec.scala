package io.paytouch.core.reports.resources.exports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.reports.entities.ExportDownload
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ExportsDownloadFSpec extends FSpec {

  abstract class ExportResourceFSpecContext extends FSpecContext with MultipleLocationFixtures

  "GET /v1/exports.download?export_id=$" in {
    "if request has valid token" in {

      "if the export belongs to the merchant" should {
        "returned signed url" in new ExportResourceFSpecContext {
          val export = Factory.export(merchant, baseUrl = Some("http://example.com/bucket/file.csv")).create

          Get(s"/v1/exports.download?export_id=${export.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            responseAs[ApiResponse[ExportDownload]]
          }
        }

        "return 400 if a base url is not available" in new ExportResourceFSpecContext {
          val export = Factory.export(merchant, baseUrl = None).create

          Get(s"/v1/exports.download?export_id=${export.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "if the export does not belong to the merchant" should {
        "return 404" in new ExportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorExport = Factory.export(competitor, baseUrl = Some("my-base-url")).create

          Get(s"/v1/exports.download?export_id=${competitorExport.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
