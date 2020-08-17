package io.paytouch.core.async.importers

import com.github.tototoshi.csv.CSVReader

package object parsers {
  implicit class RichCSVReader(val reader: CSVReader) extends AnyVal {
    // The standard library does not support duplicated headers
    def allWithDuplicatedHeaders: List[Map[String, List[String]]] = {
      val headers = reader.readNext()
      val data = headers.map { headers =>
        val lines = reader.all()
        lines.map(l => headers.zip(l)).map {
          case row =>
            row.groupBy { case (h, _) => h }.transform {
              case (_, l) =>
                l.filter { case (_, v) => v.trim.nonEmpty }.map { case (_, v) => v.trim }
            }
        }
      }
      data.getOrElse(List.empty)
    }
  }
}
