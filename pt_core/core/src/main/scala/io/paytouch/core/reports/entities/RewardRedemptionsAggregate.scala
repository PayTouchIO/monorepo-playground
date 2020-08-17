package io.paytouch.core.reports.entities

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import slick.jdbc.PositionedResult

final case class RewardRedemptionsAggregate(count: Int, value: Option[MonetaryAmount] = None)

object RewardRedemptionsAggregate {

  def getResultOrZero(count: Int, r: PositionedResult)(implicit user: UserContext) =
    getResult(count, r).getOrElse(zero)

  def getResult(count: Int, r: PositionedResult)(implicit user: UserContext) =
    for {
      value <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
    } yield RewardRedemptionsAggregate(count, value = value)

  def zero(implicit user: UserContext) =
    RewardRedemptionsAggregate(
      count = 0,
      value = Some(MonetaryAmount(0)),
    )
}
