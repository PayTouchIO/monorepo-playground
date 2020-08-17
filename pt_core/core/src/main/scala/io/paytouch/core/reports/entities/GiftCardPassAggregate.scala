package io.paytouch.core.reports.entities

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import slick.jdbc.PositionedResult

final case class GiftCardPassAggregate(
    count: Int,
    customers: Option[Int] = None,
    total: Option[MonetaryAmount] = None,
    redeemed: Option[MonetaryAmount] = None,
    unused: Option[MonetaryAmount] = None,
  )

object GiftCardPassAggregate {

  def getResultOrZero(count: Int, r: PositionedResult)(implicit user: UserContext) =
    getResult(count, r).getOrElse(zero)

  def getResult(count: Int, r: PositionedResult)(implicit user: UserContext) =
    for {
      customers <- Option(r.nextIntOption())
      redeemed <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      total <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      unused <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
    } yield GiftCardPassAggregate(count, customers, total = total, redeemed = redeemed, unused = unused)

  def zero(implicit user: UserContext) =
    GiftCardPassAggregate(
      count = 0,
      customers = Some(0),
      total = Some(MonetaryAmount(0)),
      redeemed = Some(MonetaryAmount(0)),
      unused = Some(MonetaryAmount(0)),
    )
}
