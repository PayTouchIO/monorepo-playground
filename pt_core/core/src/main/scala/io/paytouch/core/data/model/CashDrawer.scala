package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.CashDrawerStatus
import io.paytouch.core.entities.{ ResettableBigDecimal, ResettableString, ResettableUUID, ResettableZonedDateTime }

final case class CashDrawerRecord(
    id: UUID,
    merchantId: UUID,
    locationId: Option[UUID],
    userId: Option[UUID],
    employeeId: Option[UUID],
    name: Option[String],
    startingCashAmount: BigDecimal,
    endingCashAmount: Option[BigDecimal],
    cashSalesAmount: Option[BigDecimal],
    cashRefundsAmount: Option[BigDecimal],
    paidInAndOutAmount: Option[BigDecimal],
    paidInAmount: Option[BigDecimal],
    paidOutAmount: Option[BigDecimal],
    manualPaidInAmount: Option[BigDecimal],
    manualPaidOutAmount: Option[BigDecimal],
    tippedInAmount: Option[BigDecimal],
    tippedOutAmount: Option[BigDecimal],
    expectedAmount: Option[BigDecimal],
    status: CashDrawerStatus,
    startedAt: ZonedDateTime,
    endedAt: Option[ZonedDateTime],
    exportFilename: Option[String],
    printerMacAddress: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class CashDrawerUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    locationId: ResettableUUID,
    userId: ResettableUUID,
    employeeId: ResettableUUID,
    name: ResettableString,
    startingCashAmount: Option[BigDecimal],
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
    status: Option[CashDrawerStatus],
    startedAt: Option[ZonedDateTime],
    endedAt: ResettableZonedDateTime,
    exportFilename: Option[String],
    printerMacAddress: Option[String],
  ) extends SlickMerchantUpdate[CashDrawerRecord] {

  def toRecord: CashDrawerRecord = {
    require(merchantId.isDefined, s"Impossible to convert CashDrawerUpdate without a merchant id. [$this]")
    require(
      startingCashAmount.isDefined,
      s"Impossible to convert CashDrawerUpdate without a starting cash amount. [$this]",
    )
    require(status.isDefined, s"Impossible to convert CashDrawerUpdate without a status. [$this]")
    require(startedAt.isDefined, s"Impossible to convert CashDrawerUpdate without a started at. [$this]")

    CashDrawerRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      locationId = locationId,
      userId = userId,
      employeeId = employeeId,
      name = name,
      startingCashAmount = startingCashAmount.get,
      endingCashAmount = endingCashAmount,
      cashSalesAmount = cashSalesAmount,
      cashRefundsAmount = cashRefundsAmount,
      paidInAndOutAmount = paidInAndOutAmount,
      paidInAmount = paidInAmount,
      paidOutAmount = paidOutAmount,
      manualPaidInAmount = manualPaidInAmount,
      manualPaidOutAmount = manualPaidOutAmount,
      tippedInAmount = tippedInAmount,
      tippedOutAmount = tippedOutAmount,
      expectedAmount = expectedAmount,
      status = status.get,
      startedAt = startedAt.get,
      endedAt = endedAt,
      exportFilename = exportFilename,
      printerMacAddress = printerMacAddress,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CashDrawerRecord): CashDrawerRecord =
    CashDrawerRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      userId = userId.getOrElse(record.userId),
      employeeId = employeeId.getOrElse(record.employeeId),
      name = name.getOrElse(record.name),
      startingCashAmount = startingCashAmount.getOrElse(record.startingCashAmount),
      endingCashAmount = endingCashAmount.getOrElse(record.endingCashAmount),
      cashSalesAmount = cashSalesAmount.getOrElse(record.cashSalesAmount),
      cashRefundsAmount = cashRefundsAmount.getOrElse(record.cashRefundsAmount),
      paidInAndOutAmount = paidInAndOutAmount.getOrElse(record.paidInAndOutAmount),
      paidInAmount = paidInAmount.getOrElse(record.paidInAmount),
      paidOutAmount = paidOutAmount.getOrElse(record.paidOutAmount),
      manualPaidInAmount = manualPaidInAmount.getOrElse(record.manualPaidInAmount),
      manualPaidOutAmount = manualPaidOutAmount.getOrElse(record.manualPaidOutAmount),
      tippedInAmount = tippedInAmount.getOrElse(record.tippedInAmount),
      tippedOutAmount = tippedOutAmount.getOrElse(record.tippedOutAmount),
      expectedAmount = expectedAmount.getOrElse(record.expectedAmount),
      status = status.getOrElse(record.status),
      startedAt = startedAt.getOrElse(record.startedAt),
      endedAt = endedAt.getOrElse(record.endedAt),
      exportFilename = exportFilename.orElse(record.exportFilename),
      printerMacAddress = printerMacAddress.orElse(record.printerMacAddress),
      createdAt = record.createdAt,
      updatedAt = now,
    )

}
