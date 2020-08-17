package io.paytouch.core.reports.entities

import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import slick.jdbc.PositionedResult

final case class OrderItemSalesAggregate(
    count: Int,
    discounts: Option[MonetaryAmount] = None,
    grossProfits: Option[MonetaryAmount] = None,
    grossSales: Option[MonetaryAmount] = None,
    margin: Option[BigDecimal] = None,
    netSales: Option[MonetaryAmount] = None,
    quantity: Option[BigDecimal] = None,
    returnedAmount: Option[MonetaryAmount] = None,
    returnedQuantity: Option[BigDecimal] = None,
    cost: Option[MonetaryAmount] = None,
    taxable: Option[MonetaryAmount] = None,
    nonTaxable: Option[MonetaryAmount] = None,
    taxes: Option[MonetaryAmount] = None,
  )

object OrderItemSalesAggregate {
  def getResultOrZero(count: Int, r: PositionedResult)(implicit user: UserContext) =
    getResult(count, r).getOrElse(zero)

  def getResult(count: Int, r: PositionedResult)(implicit user: UserContext) =
    for {
      cost <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      discounts <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      grossProfits <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      grossSales <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      margin <- Option(r.nextBigDecimalOption())
      netSales <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      nonTaxable <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      quantity <- Option(r.nextBigDecimalOption())
      returnedAmount <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      returnedQuantity <- Option(r.nextBigDecimalOption())
      taxable <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
      taxes <- Option(MonetaryAmount.extract(r.nextBigDecimalOption()))
    } yield OrderItemSalesAggregate(
      count = count,
      cost = cost,
      discounts = discounts,
      grossProfits = grossProfits,
      margin = margin,
      netSales = netSales,
      grossSales = grossSales,
      nonTaxable = nonTaxable,
      quantity = quantity,
      returnedAmount = returnedAmount,
      returnedQuantity = returnedQuantity,
      taxable = taxable,
      taxes = taxes,
    )

  def zero(implicit user: UserContext) =
    OrderItemSalesAggregate(
      count = 0,
      discounts = Some(MonetaryAmount(0)),
      grossProfits = Some(MonetaryAmount(0)),
      grossSales = Some(MonetaryAmount(0)),
      margin = Some(0),
      netSales = Some(MonetaryAmount(0)),
      quantity = Some(0),
      returnedQuantity = Some(0),
      returnedAmount = Some(MonetaryAmount(0)),
      cost = Some(MonetaryAmount(0)),
      taxable = Some(MonetaryAmount(0)),
      nonTaxable = Some(MonetaryAmount(0)),
      taxes = Some(MonetaryAmount(0)),
    )
}
