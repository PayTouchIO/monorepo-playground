package io.paytouch.ordering.entities

import java.util.Currency

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

final case class MonetaryAmount(amount: BigDecimal, currency: Currency) {
  lazy val roundedAmount = round(amount)

  def +(other: MonetaryAmount): MonetaryAmount =
    sameCurrencyOperation(other) { (x: BigDecimal, y: BigDecimal) =>
      MonetaryAmount(x + y, currency)
    }

  def -(other: MonetaryAmount): MonetaryAmount =
    sameCurrencyOperation(other) { (x: BigDecimal, y: BigDecimal) =>
      MonetaryAmount(x - y, currency)
    }

  def *(other: MonetaryAmount): MonetaryAmount =
    sameCurrencyOperation(other) { (x: BigDecimal, y: BigDecimal) =>
      MonetaryAmount(x * y, currency)
    }

  def /(other: MonetaryAmount): MonetaryAmount =
    sameCurrencyOperation(other) { (x: BigDecimal, y: BigDecimal) =>
      MonetaryAmount(x / y, currency)
    }

  def >(other: MonetaryAmount) = sameCurrencyOperation(other)(_ > _)

  def <(other: MonetaryAmount) = sameCurrencyOperation(other)(_ < _)

  def *(otherAmount: BigDecimal): MonetaryAmount =
    MonetaryAmount(amount * otherAmount, currency)

  def /(otherAmount: BigDecimal): MonetaryAmount = {
    val newAmount: BigDecimal = Try(amount / otherAmount).getOrElse(0)
    MonetaryAmount(newAmount, currency)
  }

  val cents: BigInt =
    round(amount * 100).toBigInt

  def unary_- = MonetaryAmount(-amount, currency)

  private def sameCurrencyOperation[T](other: MonetaryAmount)(f: (BigDecimal, BigDecimal) => T) = {
    require(
      currency.getCurrencyCode == other.currency.getCurrencyCode,
      "Cannot perform operations on monetary amounts with different currencies",
    )
    f(amount, other.amount)
  }

  private def round(bd: BigDecimal) = bd.setScale(2, RoundingMode.HALF_UP)

  def show: String =
    currency.getCurrencyCode match {
      case "USD" => s"$$$roundedAmount"
      case "CAD" => s"$$$roundedAmount"
      case "GBP" => s"£$roundedAmount"
      case "EUR" => s"€$roundedAmount"
      case _     => s"$currency $roundedAmount"
    }

  def isZero: Boolean =
    amount == 0
}

object MonetaryAmount {
  def apply(amount: BigDecimal)(implicit context: AppContext): MonetaryAmount =
    MonetaryAmount(amount, context.currency)

  def extract(optAmount: Option[BigDecimal])(implicit context: AppContext): Option[MonetaryAmount] =
    extract(optAmount, context.currency)

  def extract(optAmount: Option[BigDecimal], currency: Currency): Option[MonetaryAmount] =
    optAmount.map(apply(_, currency))

  def fromCents(amount: BigInt, currency: Currency): MonetaryAmount =
    MonetaryAmount(BigDecimal(amount) / 100, currency)

  implicit class MonetaryAmountBigDecimal(val d: BigDecimal) extends AnyVal {
    def USD = MonetaryAmount(d, Currency.getInstance("USD"))
    def GBP = MonetaryAmount(d, Currency.getInstance("GBP"))
    def EUR = MonetaryAmount(d, Currency.getInstance("EUR"))
    def CAD = MonetaryAmount(d, Currency.getInstance("CAD"))

    def $$$(implicit user: UserContext) = MonetaryAmount(d, user.currency)
  }

  implicit def toMonetaryAmountBigDecimal(d: Double): MonetaryAmountBigDecimal =
    new MonetaryAmountBigDecimal(BigDecimal(d.toString))

  implicit def toMonetaryAmountBigDecimal(i: Int): MonetaryAmountBigDecimal =
    new MonetaryAmountBigDecimal(BigDecimal(i.toString))

  implicit def defaultOrdering: Ordering[MonetaryAmount] =
    Ordering.by(e => e.amount)
}
