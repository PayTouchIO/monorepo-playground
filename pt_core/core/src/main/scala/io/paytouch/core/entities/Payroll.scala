package io.paytouch.core.entities

import io.paytouch.core.entities.enums.ExposedName

final case class Payroll(
    user: UserInfo,
    totalMins: Int,
    totalDeltaMins: Int,
    totalRegularMins: Int,
    totalOvertimeMins: Int,
    hourlyRate: MonetaryAmount,
    hourlyOvertimeRate: MonetaryAmount,
    totalWage: MonetaryAmount,
    totalTips: Seq[MonetaryAmount],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Payroll
}
