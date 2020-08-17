package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.CashDrawerActivityRecord
import io.paytouch.core.data.model.enums.CashDrawerActivityType

class CashDrawerActivitiesTable(tag: Tag)
    extends SlickMerchantTable[CashDrawerActivityRecord](tag, "cash_drawer_activities") {

  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")
  def userId = column[Option[UUID]]("user_id")
  def orderId = column[Option[UUID]]("order_id")
  def cashDrawerId = column[Option[UUID]]("cash_drawer_id")
  def `type` = column[CashDrawerActivityType]("type")
  def startingCashAmount = column[Option[BigDecimal]]("starting_cash_amount")
  def endingCashAmount = column[Option[BigDecimal]]("ending_cash_amount")
  def payInAmount = column[Option[BigDecimal]]("pay_in_amount")
  def payOutAmount = column[Option[BigDecimal]]("pay_out_amount")
  def tipInAmount = column[Option[BigDecimal]]("tip_in_amount")
  def tipOutAmount = column[Option[BigDecimal]]("tip_out_amount")
  def currentBalanceAmount = column[BigDecimal]("current_balance_amount")
  def tipForUserId = column[Option[UUID]]("tip_for_user_id")
  def timestamp = column[ZonedDateTime]("timestamp")
  def notes = column[Option[String]]("notes")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      userId,
      orderId,
      cashDrawerId,
      `type`,
      startingCashAmount,
      endingCashAmount,
      payInAmount,
      payOutAmount,
      tipInAmount,
      tipOutAmount,
      currentBalanceAmount,
      tipForUserId,
      timestamp,
      notes,
      createdAt,
      updatedAt,
    ).<>(CashDrawerActivityRecord.tupled, CashDrawerActivityRecord.unapply)

}
