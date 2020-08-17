package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.CashDrawerActivityType
import io.paytouch.core.entities.enums.ExposedName

final case class CashDrawerActivity(
    id: UUID,
    userId: Option[UUID],
    orderId: Option[UUID],
    user: Option[UserInfo],
    cashDrawerId: Option[UUID],
    `type`: CashDrawerActivityType,
    startingCash: Option[MonetaryAmount],
    endingCash: Option[MonetaryAmount],
    payIn: Option[MonetaryAmount],
    payOut: Option[MonetaryAmount],
    tipIn: Option[MonetaryAmount],
    tipOut: Option[MonetaryAmount],
    currentBalance: MonetaryAmount,
    tipForUserId: Option[UUID],
    timestamp: ZonedDateTime,
    notes: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.CashDrawerActivity
}

final case class CashDrawerActivityUpsertion(
    id: UUID,
    userId: UUID,
    orderId: Option[UUID],
    cashDrawerId: UUID,
    `type`: CashDrawerActivityType,
    startingCashAmount: ResettableBigDecimal,
    endingCashAmount: ResettableBigDecimal,
    payInAmount: ResettableBigDecimal,
    payOutAmount: ResettableBigDecimal,
    tipInAmount: ResettableBigDecimal,
    tipOutAmount: ResettableBigDecimal,
    currentBalanceAmount: BigDecimal,
    tipForUserId: Option[UUID],
    timestamp: ZonedDateTime,
    notes: ResettableString,
  )
