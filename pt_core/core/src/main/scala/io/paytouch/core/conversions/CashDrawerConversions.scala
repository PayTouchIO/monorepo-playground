package io.paytouch.core.conversions

import io.paytouch.core.data.model.CashDrawerRecord
import io.paytouch.core.entities.{ MonetaryAmount, UserContext, CashDrawer => CashDrawerEntity }

trait CashDrawerConversions extends EntityConversion[CashDrawerRecord, CashDrawerEntity] {

  def fromRecordToEntity(record: CashDrawerRecord)(implicit user: UserContext): CashDrawerEntity =
    CashDrawerEntity(
      id = record.id,
      locationId = record.locationId,
      userId = record.userId,
      employeeId = record.employeeId,
      name = record.name,
      startingCash = MonetaryAmount(record.startingCashAmount),
      endingCash = record.endingCashAmount.map(amount => MonetaryAmount(amount)),
      cashSales = record.cashSalesAmount.map(amount => MonetaryAmount(amount)),
      cashRefunds = record.cashRefundsAmount.map(amount => MonetaryAmount(amount)),
      paidInAndOut = record.paidInAndOutAmount.map(amount => MonetaryAmount(amount)),
      paidIn = record.paidInAmount.map(amount => MonetaryAmount(amount)),
      paidOut = record.paidOutAmount.map(amount => MonetaryAmount(amount)),
      manualPaidIn = record.manualPaidInAmount.map(amount => MonetaryAmount(amount)),
      manualPaidOut = record.manualPaidOutAmount.map(amount => MonetaryAmount(amount)),
      tippedIn = record.tippedInAmount.map(amount => MonetaryAmount(amount)),
      tippedOut = record.tippedOutAmount.map(amount => MonetaryAmount(amount)),
      expected = record.expectedAmount.map(amount => MonetaryAmount(amount)),
      status = record.status,
      startedAt = record.startedAt,
      endedAt = record.endedAt,
      printerMacAddress = record.printerMacAddress,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )
}
