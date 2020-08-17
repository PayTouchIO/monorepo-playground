package io.paytouch.ordering.clients.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.GiftCardPass._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.ExposedName

final case class GiftCardPass(id: Id, balance: MonetaryAmount) extends ExposedEntity {
  val classShortName = ExposedName.GiftCardPass
}

final case class GiftCardPassCharge(
    giftCardPassId: Id,
    amount: BigDecimal,
  )

object GiftCardPassCharge {
  final case class Failure(
      giftCardPassId: Id,
      requestedAmount: BigDecimal,
      actualBalance: BigDecimal,
    ) extends ExposedEntity {
    override def classShortName: ExposedName =
      ExposedName.GiftCardPassChargeFailure
    override val productPrefix: String =
      "GiftCardPassCharge.Failure"
  }
}
