package io.paytouch.core.entities

import java.util.Currency

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

final case class MonetaryAmount(amount: BigDecimal, currency: Currency) {
  lazy val roundedAmount = amount.setScale(2, RoundingMode.HALF_UP)

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

  def *(otherAmount: BigDecimal): MonetaryAmount = MonetaryAmount(amount * otherAmount, currency)

  def /(otherAmount: BigDecimal): MonetaryAmount = {
    val newAmount: BigDecimal = Try(amount / otherAmount).getOrElse(0)
    MonetaryAmount(newAmount, currency)
  }

  def unary_- = MonetaryAmount(-amount, currency)

  private def sameCurrencyOperation[T](other: MonetaryAmount)(f: (BigDecimal, BigDecimal) => T) = {
    require(
      currency.getCurrencyCode == other.currency.getCurrencyCode,
      "Cannot perform operations on monetary amounts with different currencies",
    )
    f(amount, other.amount)
  }

  def show: String =
    currency.getCurrencyCode match {
      case "USD" => s"$$$roundedAmount"
      case "CAD" => s"$$$roundedAmount"
      case "GBP" => s"£$roundedAmount"
      case "EUR" => s"€$roundedAmount"
      case _     => s"$currency $roundedAmount"
    }
}

object MonetaryAmount {
  def apply(amount: BigDecimal)(implicit user: UserContext): MonetaryAmount = MonetaryAmount(amount, user.currency)

  def apply(amount: BigDecimal, merchant: MerchantContext): MonetaryAmount = MonetaryAmount(amount, merchant.currency)

  def extract(amount: Option[BigDecimal])(implicit user: UserContext): Option[MonetaryAmount] =
    extract(amount, user.toMerchantContext)

  def extract(amount: Option[BigDecimal], merchant: MerchantContext): Option[MonetaryAmount] =
    amount.map(MonetaryAmount(_, merchant))

  def extract(amount: Option[BigDecimal], currency: Currency): Option[MonetaryAmount] =
    extract(amount, Some(currency))

  def extract(amount: Option[BigDecimal], currency: Option[Currency]): Option[MonetaryAmount] =
    for { a <- amount; c <- currency } yield MonetaryAmount(a, c)

  def sum(seq: Seq[MonetaryAmount])(implicit user: UserContext): MonetaryAmount =
    seq.fold(MonetaryAmount(0, user.currency))(_ + _)

  implicit class MonetaryAmountBigDecimal(val d: BigDecimal) extends AnyVal {
    def USD = MonetaryAmount(d, Currency.getInstance("USD"))
    def GBP = MonetaryAmount(d, Currency.getInstance("GBP"))
    def EUR = MonetaryAmount(d, Currency.getInstance("EUR"))
    def CAD = MonetaryAmount(d, Currency.getInstance("CAD"))

    def $$$(implicit merchant: MerchantContext) = MonetaryAmount(d, merchant.currency)
  }

  implicit def toMonetaryAmountBigDecimal(d: Double): MonetaryAmountBigDecimal =
    new MonetaryAmountBigDecimal(BigDecimal(d.toString))
  implicit def toMonetaryAmountBigDecimal(i: Int): MonetaryAmountBigDecimal =
    new MonetaryAmountBigDecimal(BigDecimal(i.toString))

  implicit def defaultOrdering: Ordering[MonetaryAmount] = Ordering.by(_.amount)
}
