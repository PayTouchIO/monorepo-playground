package io.paytouch.core.resources.payroll

import java.util.UUID

import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.entities.{ Payroll => PayrollEntity, _ }
import io.paytouch.core.utils._

abstract class PayrollFSpec extends FSpec {

  abstract class PayrollResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val userDao = daos.userDao

    def assertResponse(
        record: UserRecord,
        entity: PayrollEntity,
        totalMins: Option[Int] = None,
        totalDeltaMins: Option[Int] = None,
        totalWage: Option[MonetaryAmount] = None,
        totalTips: Option[Seq[MonetaryAmount]] = None,
        totalRegularMins: Option[Int] = None,
        totalOtMins: Option[Int] = None,
      ) = {
      assertUserResponse(record.id, entity.user)
      entity.hourlyRate ==== MonetaryAmount(record.hourlyRateAmount.getOrElse[BigDecimal](0), currency)
      entity.hourlyOvertimeRate ==== MonetaryAmount(record.overtimeRateAmount.getOrElse[BigDecimal](0), currency)

      if (totalMins.isDefined) totalMins ==== Some(entity.totalMins)
      if (totalDeltaMins.isDefined) totalDeltaMins ==== Some(entity.totalDeltaMins)
      if (totalWage.isDefined) totalWage ==== Some(entity.totalWage)
      if (totalTips.isDefined) totalTips ==== Some(entity.totalTips)
      if (totalRegularMins.isDefined) totalRegularMins ==== Some(entity.totalRegularMins)
      if (totalOtMins.isDefined) totalOtMins ==== Some(entity.totalOvertimeMins)
    }

    def assertUserResponse(userId: UUID, userEntity: UserInfo) = {
      val userRecord = userDao.findById(userId).await.get
      userId ==== userEntity.id
      userRecord.firstName ==== userEntity.firstName
      userRecord.lastName ==== userEntity.lastName
      userRecord.email ==== userEntity.email
    }
  }
}
