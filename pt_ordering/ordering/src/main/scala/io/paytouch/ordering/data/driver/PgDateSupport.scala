package io.paytouch.ordering.data.driver

import com.github.tminglei.slickpg._
import io.paytouch.ordering.utils.Formatters
import slick.jdbc.PostgresProfile

trait PgDateSupport extends PostgresProfile with PgDate2Support {

  trait DateTimeFormatters extends Date2DateTimeFormatters {
    override val date2TzDateTimeFormatter = Formatters.TimestampFormatter
  }

  trait DateImplicits extends DateTimeImplicits with DateTimeFormatters

  trait DatePlainImplicits extends DateTimeImplicitsPeriod with DateTimeFormatters
}
