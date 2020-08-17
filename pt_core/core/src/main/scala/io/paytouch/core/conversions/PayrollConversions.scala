package io.paytouch.core.conversions

import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.{ TimeCardTotals, UserRecord }
import io.paytouch.core.entities.{ MonetaryAmount, UserContext, UserInfo, Payroll => PayrollEntity }

import scala.math.max

trait PayrollConversions {

  def fromRecordsAndOptionsToEntities(
      records: Seq[UserRecord],
      totalsByUser: Map[UUID, TimeCardTotals],
      totalTipsByUser: Map[UUID, Seq[MonetaryAmount]],
    )(implicit
      user: UserContext,
    ) =
    records.map { record =>
      val userInfo = record.toUserInfo
      val totals = totalsByUser.getOrElse(record.id, TimeCardTotals.zero)
      val hourlyRate = MonetaryAmount(record.hourlyRateAmount.getOrElse[BigDecimal](0))
      val hourlyOvertimeRate = MonetaryAmount(record.overtimeRateAmount.getOrElse[BigDecimal](0))
      val totalTips = totalTipsByUser.getOrElse(record.id, Seq.empty)

      val totalWage = (hourlyRate * (totals.regularMins / 60.0)) + (hourlyOvertimeRate * max(
        totals.overtimeMins / 60.0,
        0,
      ))

      fromRecordToEntity(userInfo, totals, hourlyRate, hourlyOvertimeRate, totalWage, totalTips)
    }

  def fromRecordToEntity(
      record: UserInfo,
      totals: TimeCardTotals,
      hourlyRate: MonetaryAmount,
      hourlyOvertimeRate: MonetaryAmount,
      totalWage: MonetaryAmount,
      totalTips: Seq[MonetaryAmount],
    ): PayrollEntity =
    PayrollEntity(
      user = record,
      totalMins = totals.totalMins,
      totalDeltaMins = totals.deltaMins,
      totalRegularMins = totals.regularMins,
      totalOvertimeMins = totals.overtimeMins,
      hourlyRate = hourlyRate,
      hourlyOvertimeRate = hourlyOvertimeRate,
      totalWage = totalWage,
      totalTips = totalTips,
    )
}
