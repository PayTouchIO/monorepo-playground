package io.paytouch.core.reports.resources.exports

import java.time.LocalDateTime

import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.Export
import io.paytouch.core.reports.entities.enums.CsvExports
import io.paytouch.core.utils.{ DefaultFixtures, Formatters, UserFixtures, UtcTime }

class ExportsSingleViewFSpec extends ExportsCreateFSpec {

  abstract class ExportsSingleViewContext extends FSpecContext with DefaultFixtures {
    val exportDao = daos.exportDao

    def assertUrl(export: ExportRecord, reportType: String) = {
      val baseBucketUrl = "https://s3.amazonaws.com/my-test-export-bucket"
      val fileName = {
        val now = UtcTime.now.format(Formatters.LocalDateFormatter)

        def base(middle: String) =
          s"foobar_$middle.csv"

        export.`type` match {
          case "cash_drawers" =>
            base(s"${now}_${now}")

          case _ =>
            base(now)
        }
      }
      val expectedUrl = List(baseBucketUrl, merchant.id, reportType, export.id, fileName).mkString("/")
      export.baseUrl ==== Some(expectedUrl)
    }
  }

  CsvExports.values.map { csvExport =>
    s"POST /v1/exports/${csvExport.entryName}.single" in {
      "if request has valid token" in {

        "create and process a single customers export" in new ExportsSingleViewContext {
          val uri = {
            val base = s"/v1/exports/${csvExport.entryName}.single?filename=foobar"

            csvExport match {
              case CsvExports.CashDrawers =>
                val now = LocalDateTime.now

                s"$base&from=${now}&to=${now}"

              case _ =>
                base
            }
          }

          Post(uri)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val export = responseAs[ApiResponse[Export]].data
            afterAWhile {
              val record = exportDao.findById(export.id).await.get
              record.status ==== ExportStatus.Completed
              assertUrl(record, csvExport.entryName)
            }
          }
        }
      }
    }

  }
}
