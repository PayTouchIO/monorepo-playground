package io.paytouch.core.reports.entities

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import slick.jdbc.PositionedResult

final case class LoyaltyOrdersAggregate(count: Int, amount: Option[MonetaryAmount] = None)

object LoyaltyOrdersAggregate {

  def getResultOrZero(count: Int, r: PositionedResult)(implicit user: UserContext) =
    getResult(count, r).getOrElse(zero)

  def getResult(count: Int, r: PositionedResult)(implicit user: UserContext) =
    for {
      amount <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
    } yield LoyaltyOrdersAggregate(count, amount = amount)

  def zero(implicit user: UserContext) =
    LoyaltyOrdersAggregate(
      count = 0,
      amount = Some(MonetaryAmount(0)),
    )
}
