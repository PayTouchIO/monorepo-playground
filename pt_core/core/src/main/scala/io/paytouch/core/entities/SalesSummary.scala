package io.paytouch.core.entities

final case class SalesSummary(totalGross: Seq[MonetaryAmount], totalNet: Seq[MonetaryAmount])

object SalesSummary {

  def apply(totalGrossAmount: BigDecimal, totalNetAmount: BigDecimal)(implicit user: UserContext): SalesSummary = {
    val totalGross = Seq(MonetaryAmount(totalGrossAmount))
    val totalNet = Seq(MonetaryAmount(totalNetAmount))
    apply(totalGross, totalNet)
  }
}
