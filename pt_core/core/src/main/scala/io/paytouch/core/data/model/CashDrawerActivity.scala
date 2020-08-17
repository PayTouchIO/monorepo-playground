package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.CashDrawerActivityType
import io.paytouch.core.entities.{ ResettableBigDecimal, ResettableString, ResettableUUID }

final case class CashDrawerActivityRecord(
    id: UUID,
    merchantId: UUID,
    userId: Option[UUID],
    orderId: Option[UUID],
    cashDrawerId: Option[UUID],
    `type`: CashDrawerActivityType,
    startingCashAmount: Option[BigDecimal],
    endingCashAmount: Option[BigDecimal],
    payInAmount: Option[BigDecimal],
    payOutAmount: Option[BigDecimal],
    tipInAmount: Option[BigDecimal],
    tipOutAmount: Option[BigDecimal],
    currentBalanceAmount: BigDecimal,
    tipForUserId: Option[UUID],
    timestamp: ZonedDateTime,
    notes: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class CashDrawerActivityUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: ResettableUUID,
    orderId: Option[UUID],
    cashDrawerId: ResettableUUID,
    `type`: Option[CashDrawerActivityType],
    startingCashAmount: ResettableBigDecimal,
    endingCashAmount: ResettableBigDecimal,
    payInAmount: ResettableBigDecimal,
    payOutAmount: ResettableBigDecimal,
    tipInAmount: ResettableBigDecimal,
    tipOutAmount: ResettableBigDecimal,
    currentBalanceAmount: Option[BigDecimal],
    tipForUserId: Option[UUID],
    timestamp: Option[ZonedDateTime],
    notes: ResettableString,
  ) extends SlickMerchantUpdate[CashDrawerActivityRecord] {

  def toRecord: CashDrawerActivityRecord = {
    require(merchantId.isDefined, s"Impossible to convert CashDrawerActivityUpdate without a merchant id. [$this]")
    require(`type`.isDefined, s"Impossible to convert CashDrawerActivityUpdate without a `type`. [$this]")
    require(
      currentBalanceAmount.isDefined,
      s"Impossible to convert CashDrawerActivityUpdate without a current balance amount. [$this]",
    )
    require(timestamp.isDefined, s"Impossible to convert CashDrawerActivityUpdate without a timestamp. [$this]")

    CashDrawerActivityRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId,
      orderId = orderId,
      cashDrawerId = cashDrawerId,
      `type` = `type`.get,
      startingCashAmount = startingCashAmount,
      endingCashAmount = endingCashAmount,
      payInAmount = payInAmount,
      payOutAmount = payOutAmount,
      tipInAmount = tipInAmount,
      tipOutAmount = tipOutAmount,
      currentBalanceAmount = currentBalanceAmount.get,
      tipForUserId = tipForUserId,
      timestamp = timestamp.get,
      notes = notes,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CashDrawerActivityRecord): CashDrawerActivityRecord =
    CashDrawerActivityRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      orderId = orderId.orElse(record.orderId),
      cashDrawerId = cashDrawerId.getOrElse(record.cashDrawerId),
      `type` = `type`.getOrElse(record.`type`),
      startingCashAmount = startingCashAmount.getOrElse(record.startingCashAmount),
      endingCashAmount = endingCashAmount.getOrElse(record.endingCashAmount),
      payInAmount = payInAmount.getOrElse(record.payInAmount),
      payOutAmount = payOutAmount.getOrElse(record.payOutAmount),
      tipInAmount = tipInAmount.getOrElse(record.tipInAmount),
      tipOutAmount = tipOutAmount.getOrElse(record.tipOutAmount),
      currentBalanceAmount = currentBalanceAmount.getOrElse(record.currentBalanceAmount),
      tipForUserId = tipForUserId.orElse(record.tipForUserId),
      timestamp = timestamp.getOrElse(record.timestamp),
      notes = notes.getOrElse(record.notes),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
