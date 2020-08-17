package io.paytouch.core.utils

import java.time.ZoneId
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField
import java.util.Currency

import io.paytouch.core.utils.RichString._
import io.paytouch.core.entities.Weekdays

import scala.util.Try

object Formatters {
  val ZonedDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  val LocalDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  val LocalTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
  val LocalDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  val TimestampFormatter = new DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    .optionalStart()
    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
    .optionalEnd()
    .appendOffset("+HH:mm", "")
    .toFormatter()

  val CompactDateFormatter = new DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ofPattern("yyyyMMdd"))
    .toFormatter()

  implicit class RichDateTimeFormatter(val dtf: DateTimeFormatter) extends AnyVal {
    def canParse(x: String): Boolean = Try(dtf.parse(x)).isSuccess
  }

  def isValidZoneId(x: String): Boolean = Try(ZoneId.of(x)).isSuccess

  def isValidCurrency(x: String): Boolean = Try(Currency.getInstance(x.toUpperCase)).isSuccess

  def isValidDay(x: String): Boolean = Try(Weekdays.withName(_pascalize(x))).isSuccess

  def isValidBigDecimal(x: String): Boolean = Try(BigDecimal(x)).isSuccess

  def isValidInt(x: String): Boolean = Try(x.toInt).isSuccess

  def isValidEmail(x: String): Boolean = x.contains("@") && x.contains(".")
}
