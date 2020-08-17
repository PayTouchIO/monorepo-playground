package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.CashDrawerStatus
import io.paytouch.core.entities.enums.ExposedName

final case class CashDrawer(
    id: UUID,
    locationId: Option[UUID],
    userId: Option[UUID],
    employeeId: Option[UUID],
    name: Option[String],
    startingCash: MonetaryAmount,
    endingCash: Option[MonetaryAmount],
    cashSales: Option[MonetaryAmount],
    cashRefunds: Option[MonetaryAmount],
    paidInAndOut: Option[MonetaryAmount],
    paidIn: Option[MonetaryAmount],
    paidOut: Option[MonetaryAmount],
    manualPaidIn: Option[MonetaryAmount],
    manualPaidOut: Option[MonetaryAmount],
    tippedIn: Option[MonetaryAmount],
    tippedOut: Option[MonetaryAmount],
    expected: Option[MonetaryAmount],
    status: CashDrawerStatus,
    startedAt: ZonedDateTime,
    endedAt: Option[ZonedDateTime],
    printerMacAddress: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.CashDrawer
}

final case class CashDrawerUpsertion(
    locationId: UUID,
    userId: UUID,
    employeeId: Option[UUID],
    name: ResettableString,
    startingCashAmount: ResettableBigDecimal,
    endingCashAmount: ResettableBigDecimal,
    cashSalesAmount: ResettableBigDecimal,
    cashRefundsAmount: ResettableBigDecimal,
    paidInAndOutAmount: ResettableBigDecimal,
    paidInAmount: ResettableBigDecimal,
    paidOutAmount: ResettableBigDecimal,
    manualPaidInAmount: ResettableBigDecimal,
    manualPaidOutAmount: ResettableBigDecimal,
    tippedInAmount: ResettableBigDecimal,
    tippedOutAmount: ResettableBigDecimal,
    expectedAmount: ResettableBigDecimal,
    status: CashDrawerStatus,
    startedAt: ZonedDateTime,
    endedAt: ResettableZonedDateTime,
    printerMacAddress: Option[String],
    appendActivities: Option[Seq[CashDrawerActivityUpsertion]],
  )
