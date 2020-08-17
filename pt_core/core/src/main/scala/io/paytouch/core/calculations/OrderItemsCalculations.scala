package io.paytouch.core.calculations

import io.paytouch.core.data.model.OrderItemRecord
import io.paytouch.core.data.model.enums.UnitType
import io.paytouch.core.entities.{ MonetaryAmount, ProductQuantity, UserContext }

import scala.util.Try

trait OrderItemsCalculations extends MarginCalculation {

  def computeAvgPrice(items: Seq[OrderItemRecord])(implicit user: UserContext): MonetaryAmount =
    computeAvg(items)(_.priceAmount)

  def computeAvgDiscount(items: Seq[OrderItemRecord])(implicit user: UserContext): MonetaryAmount =
    computeAvg(items)(_.discountAmount)

  def computeAvgCost(items: Seq[OrderItemRecord])(implicit user: UserContext): MonetaryAmount =
    computeAvg(items)(_.costAmount)

  def computeAvgMargin(items: Seq[OrderItemRecord])(implicit user: UserContext): BigDecimal = {
    val avgPrice = computeAvgPrice(items)
    val avgCost = computeAvgCost(items)

    computeMargin(priceAmount = avgPrice.amount, costAmount = avgCost.amount)
  }

  def computeTotalSold(items: Seq[OrderItemRecord]): ProductQuantity = {
    val unit = items.headOption.flatMap(_.unit).getOrElse(UnitType.`Unit`)
    val amount = items.map(_.quantity.getOrElse[BigDecimal](0)).sum
    ProductQuantity(amount, unit)
  }

  def computeTotalRevenue(items: Seq[OrderItemRecord])(implicit user: UserContext): MonetaryAmount = {
    val totalPrice = computeSumByQuantity(items)(_.priceAmount)
    val totalDiscount = computeSumByQuantity(items)(_.discountAmount)

    totalPrice - totalDiscount
  }

  def computeTotalTax(items: Seq[OrderItemRecord])(implicit user: UserContext): MonetaryAmount =
    computeSumByQuantity(items)(_.taxAmount)

  def computeTotalProfit(items: Seq[OrderItemRecord])(implicit user: UserContext): MonetaryAmount = {
    val totalPrice = computeSum(items)(_.totalPriceAmount)
    val totalCost = computeSumByQuantity(items)(_.costAmount)
    val totalDiscount = computeSum(items)(_.discountAmount)
    val totalTax = computeSum(items)(_.taxAmount)

    totalPrice - totalCost - totalDiscount - totalTax
  }

  private def computeAvg(
      items: Seq[OrderItemRecord],
    )(
      f: OrderItemRecord => Option[BigDecimal],
    )(implicit
      user: UserContext,
    ): MonetaryAmount =
    computeOp(items)(
      f,
      { amountsWithQnt =>
        val amounts = amountsWithQnt.map { case (amm, qnt) => amm * qnt }
        val count = amountsWithQnt.map { case (_, qnt) => qnt }.sum
        Try(amounts.sum / count) getOrElse 0
      },
    )

  private def computeSum(
      items: Seq[OrderItemRecord],
    )(
      f: OrderItemRecord => Option[BigDecimal],
    )(implicit
      user: UserContext,
    ): MonetaryAmount =
    computeOp(items)(
      f,
      { amountsWithQnt =>
        val amounts = amountsWithQnt.map { case (amm, _) => amm }
        amounts.sum
      },
    )

  private def computeSumByQuantity(
      items: Seq[OrderItemRecord],
    )(
      f: OrderItemRecord => Option[BigDecimal],
    )(implicit
      user: UserContext,
    ): MonetaryAmount =
    computeOp(items)(
      f,
      { amountsWithQnt =>
        val amounts = amountsWithQnt.map { case (amm, qnt) => amm * qnt }
        amounts.sum
      },
    )

  private def computeOp(
      items: Seq[OrderItemRecord],
    )(
      f: OrderItemRecord => Option[BigDecimal],
      op: Seq[(BigDecimal, BigDecimal)] => BigDecimal,
    )(implicit
      user: UserContext,
    ): MonetaryAmount = {
    val moneyWithQnt = extractMonetaryAmounts(items)(f)
    val currency =
      moneyWithQnt.headOption.map { case (monetaryAmount, _) => monetaryAmount.currency }.getOrElse(user.currency)
    val amountsWithQnt = moneyWithQnt.map { case (moneyAmount, qnt) => (moneyAmount.amount, qnt) }
    val amount = op(amountsWithQnt)
    MonetaryAmount(amount, currency)
  }

  private def extractMonetaryAmounts(
      items: Seq[OrderItemRecord],
    )(
      f: OrderItemRecord => Option[BigDecimal],
    )(implicit
      user: UserContext,
    ): Seq[(MonetaryAmount, BigDecimal)] =
    items.map { item =>
      val quantity = item.quantity.getOrElse[BigDecimal](0)
      val amount = f(item)
      MonetaryAmount(amount.getOrElse[BigDecimal](0)) -> quantity
    }
}
