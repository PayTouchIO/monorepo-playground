package io.paytouch.core.reports.resources.exports

import io.paytouch.core.reports.data.daos.ExportDao
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.reports.resources.ReportsDates
import io.paytouch.core.utils.{ FSpec, UserFixtures }

trait ExportsCreateFSpec extends FSpec {

  trait ExportsFSpecContext extends FSpecContext with UserFixtures { self: ReportsDates =>
    lazy val defaultBaseParams = s"$defaultParamsNoInterval&filename=foobar"

    def exportDao: ExportDao

    def assertBaseUrl(export: ExportRecord, reportType: String) = {
      val baseBucketUrl = "https://s3.amazonaws.com/my-test-export-bucket"
      val fileName = s"foobar_2015-12-01_2015-12-31.csv"
      val expectedUrl = List(baseBucketUrl, merchant.id, reportType, export.id, fileName).mkString("/")
      export.baseUrl ==== Some(expectedUrl)
    }
  }
}
