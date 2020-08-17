package io.paytouch.core.reports.async.exporters

import java.io.File
import java.util.UUID

import com.github.tototoshi.csv.{ CSVReader, CSVWriter }
import io.paytouch.core.reports.entities.ReportResponse
import io.paytouch.core.reports.filters.ReportFilters

import scala.concurrent._

class CSVConverter(implicit val ec: ExecutionContext) {

  def createTempFile: Future[File] = Future(File.createTempFile(s"export-${UUID.randomUUID}", "csv"))

  def convertToData[T](
      report: ReportResponse[T],
      filters: ReportFilters,
    )(implicit
      converter: CSVConverterHelper[ReportResponse[T]],
    ): Future[(List[String], List[List[String]])] =
    Future {
      val header = converter.header(Seq(report), filters)
      val rows = converter.rows(report, filters)
      (header, rows)
    }

  def addRows[T](
      file: File,
      rows: List[List[String]],
      append: Boolean = true,
    ): Future[Unit] =
    Future {
      val writer = CSVWriter.open(file, append)
      try writer.writeAll(rows)
      finally writer.close()
    }

  def prependHeaderAndRemoveIgnorableColumns(file: File, header: List[String]): Future[Unit] =
    Future {
      val reader = CSVReader.open(file)
      try {
        val rows = reader.all()
        val data = List(header) ++ rows
        val cleanData = CSVConverterHelper.removeIgnorableColumns(data)
        addRows(file, cleanData, append = false)
      }
      finally reader.close()
    }
}
