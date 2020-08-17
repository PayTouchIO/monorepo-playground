package io.paytouch.core.reports.entities.enums

import java.time.temporal.ChronoUnit
import java.time.{ Duration, LocalDateTime, Period }

import enumeratum._
import io.paytouch.core.errors.ExceededIntervalsInTimeRange
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.{ EnumEntrySnake, Multiple }

sealed abstract class ReportInterval(val unit: ChronoUnit) extends EnumEntrySnake {
  import ReportInterval._

  def step(from: LocalDateTime, to: LocalDateTime): String =
    this match {
      case Hourly  => "1 hours"
      case Daily   => "1 days"
      case Weekly  => "1 weeks"
      case Monthly => "1 months"
      case Yearly  => "1 years"
      case NoInterval =>
        val diffSeconds = Duration.between(from, to).getSeconds + 1
        s"$diffSeconds seconds"
    }
}

case object ReportInterval extends Enum[ReportInterval] {

  case object Hourly extends ReportInterval(ChronoUnit.HOURS)
  case object Daily extends ReportInterval(ChronoUnit.DAYS)
  case object Weekly extends ReportInterval(ChronoUnit.WEEKS)
  case object Monthly extends ReportInterval(ChronoUnit.MONTHS)
  case object Yearly extends ReportInterval(ChronoUnit.YEARS)
  case object NoInterval extends ReportInterval(ChronoUnit.FOREVER)

  val values = findValues

  def validateAndDetectInterval(
      from: LocalDateTime,
      to: LocalDateTime,
      withInterval: Boolean,
      forceInterval: Option[ReportInterval],
    ): ErrorsOr[ReportInterval] =
    forceInterval match {
      case Some(interval) => validatedForcedInterval(interval, from, to)
      case _              => Multiple.success(detectInterval(from, to, withInterval))
    }

  def detectInterval(
      from: LocalDateTime,
      to: LocalDateTime,
      withInterval: Boolean,
    ): ReportInterval =
    if (withInterval) chooseBestIntervalForRange(from, to) else ReportInterval.NoInterval

  private def validatedForcedInterval(
      requestedInterval: ReportInterval,
      from: LocalDateTime,
      to: LocalDateTime,
    ): ErrorsOr[ReportInterval] =
    requestedInterval match {
      case NoInterval => Multiple.success(NoInterval)
      case Yearly     => validateIntervalsPerRange(Yearly, from, to, 10)
      case interval =>
        val maxIntervals = 400
        validateIntervalsPerRange(interval, from, to, maxIntervals)
    }

  private def validateIntervalsPerRange(
      requestedInterval: ReportInterval,
      from: LocalDateTime,
      to: LocalDateTime,
      maxUnit: Int,
    ): ErrorsOr[ReportInterval] = {
    val intervalsInRange = requestedInterval.unit.between(from, to)
    if (intervalsInRange <= maxUnit) Multiple.success(requestedInterval)
    else Multiple.failure(ExceededIntervalsInTimeRange(intervalsInRange, maxUnit))
  }

  private def chooseBestIntervalForRange(from: LocalDateTime, to: LocalDateTime) =
    Period.between(from.toLocalDate, to.toLocalDate) match {
      case p if 2 < p.getYears                                          => ReportInterval.Yearly
      case p if (0 < p.getYears && p.getYears <= 2) || 2 < p.getMonths  => ReportInterval.Monthly
      case p if (0 < p.getMonths && p.getMonths <= 2) || 14 < p.getDays => ReportInterval.Weekly
      case p if 2 < p.getDays && p.getDays <= 14                        => ReportInterval.Daily
      case p if p.getDays <= 2                                          => ReportInterval.Hourly
    }
}
