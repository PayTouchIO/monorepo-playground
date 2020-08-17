package io.paytouch.core.reports.entities

import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import slick.jdbc.PositionedResult

final case class SalesAggregate(
    count: Int,
    costs: Option[MonetaryAmount] = None,
    discounts: Option[MonetaryAmount] = None,
    giftCardSales: Option[MonetaryAmount] = None,
    grossProfits: Option[MonetaryAmount] = None,
    grossSales: Option[MonetaryAmount] = None,
    netSales: Option[MonetaryAmount] = None,
    nonTaxable: Option[MonetaryAmount] = None,
    refunds: Option[MonetaryAmount] = None,
    taxable: Option[MonetaryAmount] = None,
    taxes: Option[MonetaryAmount] = None,
    tips: Option[MonetaryAmount] = None,
    tenderTypes: Option[Map[TransactionPaymentType, MonetaryAmount]] = None,
  )

object SalesAggregate {
  def getResultOrZero(count: Int, r: PositionedResult)(implicit user: UserContext) =
    getResult(count, r).getOrElse(zero())

  def getResult(count: Int, r: PositionedResult)(implicit user: UserContext) =
    for {
      costs <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      discounts <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      giftCardSales <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      grossProfits <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      grossSales <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      netSales <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      nonTaxableAmount <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      refunds <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      taxableAmount <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      taxes <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      tips <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
    } yield SalesAggregate(
      count = count,
      costs = costs,
      discounts = discounts,
      giftCardSales = giftCardSales,
      grossProfits = grossProfits,
      grossSales = grossSales,
      netSales = netSales,
      nonTaxable = nonTaxableAmount,
      refunds = refunds,
      taxable = taxableAmount,
      taxes = taxes,
      tips = tips,
    )

  def zero(withTenderTypes: Boolean = false)(implicit user: UserContext) =
    SalesAggregate(
      count = 0,
      costs = Some(MonetaryAmount(0)),
      discounts = Some(MonetaryAmount(0)),
      giftCardSales = Some(MonetaryAmount(0)),
      grossProfits = Some(MonetaryAmount(0)),
      grossSales = Some(MonetaryAmount(0)),
      netSales = Some(MonetaryAmount(0)),
      nonTaxable = Some(MonetaryAmount(0)),
      refunds = Some(MonetaryAmount(0)),
      taxable = Some(MonetaryAmount(0)),
      taxes = Some(MonetaryAmount(0)),
      tips = Some(MonetaryAmount(0)),
      tenderTypes =
        if (withTenderTypes) Some(TransactionPaymentType.reportValues.map(_ -> MonetaryAmount(0)).toMap) else None,
    )
}
