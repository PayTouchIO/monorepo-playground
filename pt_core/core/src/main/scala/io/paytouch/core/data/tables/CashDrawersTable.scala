package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OptLocationIdColumn
import io.paytouch.core.data.model.CashDrawerRecord
import io.paytouch.core.data.model.enums.CashDrawerStatus
import shapeless.{ Generic, HNil }
import slickless._

class CashDrawersTable(tag: Tag)
    extends SlickMerchantTable[CashDrawerRecord](tag, "cash_drawers")
       with OptLocationIdColumn {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def locationId = column[Option[UUID]]("location_id")
  def userId = column[Option[UUID]]("user_id")
  def employeeId = column[Option[UUID]]("employee_id")
  def name = column[Option[String]]("name")
  def startingCashAmount = column[BigDecimal]("starting_cash_amount")
  def endingCashAmount = column[Option[BigDecimal]]("ending_cash_amount")
  def cashSalesAmount = column[Option[BigDecimal]]("cash_sales_amount")
  def cashRefundsAmount = column[Option[BigDecimal]]("cash_refunds_amount")
  def paidInAndOutAmount = column[Option[BigDecimal]]("paid_in_and_out_amount")
  def paidInAmount = column[Option[BigDecimal]]("paid_in_amount")
  def paidOutAmount = column[Option[BigDecimal]]("paid_out_amount")
  def manualPaidInAmount = column[Option[BigDecimal]]("manual_paid_in_amount")
  def manualPaidOutAmount = column[Option[BigDecimal]]("manual_paid_out_amount")
  def tippedInAmount = column[Option[BigDecimal]]("tipped_in_amount")
  def tippedOutAmount = column[Option[BigDecimal]]("tipped_out_amount")
  def expectedAmount = column[Option[BigDecimal]]("expected_amount")
  def status = column[CashDrawerStatus]("status")
  def startedAt = column[ZonedDateTime]("started_at")
  def endedAt = column[Option[ZonedDateTime]]("ended_at")
  def exportFilename = column[Option[String]]("export_filename")
  def printerMacAddress = column[Option[String]]("printer_mac_address")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id ::
        merchantId ::
        locationId ::
        userId ::
        employeeId ::
        name ::
        startingCashAmount ::
        endingCashAmount ::
        cashSalesAmount ::
        cashRefundsAmount ::
        paidInAndOutAmount ::
        paidInAmount ::
        paidOutAmount ::
        manualPaidInAmount ::
        manualPaidOutAmount ::
        tippedInAmount ::
        tippedOutAmount ::
        expectedAmount ::
        status ::
        startedAt ::
        endedAt ::
        exportFilename ::
        printerMacAddress ::
        createdAt ::
        updatedAt ::
        HNil
    ).mappedWith(Generic[CashDrawerRecord])

}
