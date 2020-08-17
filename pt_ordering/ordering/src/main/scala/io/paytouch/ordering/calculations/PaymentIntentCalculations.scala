package io.paytouch.ordering.calculations

import io.paytouch.ordering.entities.{ RapidoOrderContext => Context, PaymentIntentUpsertion }

import scala.concurrent.Future
import RoundingUtils._

final case class PaymentIntentCalculationsResult(
    subtotalAmount: BigDecimal,
    taxAmount: BigDecimal,
    tipAmount: BigDecimal,
    totalAmount: BigDecimal,
  )

trait PaymentIntentCalculations {
  protected def priceCalculationsOnCreation(
      creation: PaymentIntentUpsertion,
    )(implicit
      context: Context,
    ): PaymentIntentCalculationsResult = {
    // For v1 we only allow the entire order to be paid, so we take the amounts from the order itself.
    val tipAmount = creation.tipAmount.nonNegative
    val subtotalAmount = context.order.subtotal.map(_.amount).getOrElse(BigDecimal(0)).nonNegative
    val taxAmount = context.order.tax.map(_.amount).getOrElse(BigDecimal(0)).nonNegative
    val totalAmount = context.order.total.map(_.amount).getOrElse(BigDecimal(0)).nonNegative + tipAmount

    PaymentIntentCalculationsResult(
      subtotalAmount = subtotalAmount.asRounded,
      taxAmount = taxAmount.asRounded,
      tipAmount = tipAmount.asRounded,
      totalAmount = totalAmount.asRounded,
    )
  }
}
