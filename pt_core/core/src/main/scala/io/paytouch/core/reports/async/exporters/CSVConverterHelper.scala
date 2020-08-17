package io.paytouch.core.reports.async.exporters

import java.time.{ LocalDateTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.utils.{ EnumEntrySnake, Formatters }

import scala.math.BigDecimal.RoundingMode

trait CSVConverterHelper[T] {

  type Data = T

  def header(t: Seq[T], f: ReportFilters): List[String]

  def rows(t: T, f: ReportFilters): List[List[String]]

  implicit def fromOptToString(opt: Option[String]): String = opt.getOrElse("")

  implicit def fromOptIntToString(opt: Option[Int]): String = fromOptToString(opt.map(fromIntToString))

  implicit def fromOptMonetaryToString(opt: Option[MonetaryAmount]): String =
    fromOptToString(opt.map(fromMonetaryToString))

  implicit def fromZonedDateTimeToString(date: ZonedDateTime): String = Formatters.ZonedDateTimeFormatter.format(date)

  implicit def fromLocalDateTimeToString(date: LocalDateTime): String = Formatters.LocalDateTimeFormatter.format(date)

  implicit def fromIntToString(n: Int): String = n.toString

  implicit def fromUUIDToString(uuid: UUID): String = uuid.toString

  implicit def fromBigDecimalToString(bd: BigDecimal): String = bd.setScale(2, RoundingMode.HALF_EVEN).toString

  implicit def fromOptBigDecimalToString(opt: Option[BigDecimal]): String =
    fromOptToString(opt.map(fromBigDecimalToString))

  implicit def fromMonetaryToString(monetary: MonetaryAmount): String = fromBigDecimalToString(monetary.amount)
}

trait CSVWithOrderableColumnsConverterHelper[T] extends CSVConverterHelper[T] {

  val columns: Seq[Column[T]]

  def column(
      field: EnumEntrySnake,
      rowExtractor: T => String,
      headerOverride: Option[String] = None,
    ) =
    Column[T](field, rowExtractor, headerOverride)

  lazy val columnMap = columns.groupBy(_.field.entryName)

  def header(ts: Seq[Data], f: ReportFilters): List[String] =
    f.fieldsInCsv
      .distinct
      .flatMap(field => columnMap.getOrElse(field, Seq.empty[Column[T]]).map(_.header))
      .toList

  def rows(t: Data, f: ReportFilters): List[List[String]] = {
    val fields = f
      .fieldsInCsv
      .distinct
      .flatMap(field => columnMap.getOrElse(field, Seq.empty[Column[T]]).map(_.rowExtractor(t)).toList)
      .toList
    List(fields)
  }
}

object CSVConverterHelper {

  def removeIgnorableColumns(data: List[List[String]]): List[List[String]] = {
    val transposed = uniform(data).transpose
    val transposedClean = transposed.filter(_.exists(_.nonEmpty))
    transposedClean.transpose
  }

  private def uniform(data: List[List[String]]): List[List[String]] = {
    val size = data.map(_.size).max
    data.map { d =>
      val diff = size - d.size
      val addendum = (1 to diff).map(_ => "").toList
      d ++ addendum
    }
  }
}
