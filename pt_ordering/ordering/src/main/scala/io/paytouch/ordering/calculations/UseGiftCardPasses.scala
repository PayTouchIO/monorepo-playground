package io.paytouch.ordering.calculations

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering._

object UseGiftCardPasses {
  def apply(
      appliedGiftCardPasses: Seq[entities.GiftCardPassApplied],
      totalAmountSoFarForNonGiftCardCartItems: BigDecimal,
    )(implicit
      store: entities.StoreContext,
    ): Result =
    appliedGiftCardPasses
      .sortBy(_.addedAt)
      .map(_.copy(amountToCharge = None)) // start calculation from scratch
      .foldLeft(Result(totalAmountSoFarForNonGiftCardCartItems, Vector.empty)) {
        case (Result(total, appliedPasses), currentPass) =>
          if (total == 0) // keep walking
            Result(total, appliedPasses :+ currentPass)
          else {
            val (newTotal, newAmountToCharge) =
              if (currentPass.balance.amount > total) // charge only total
                (0: BigDecimal, total)
              else // charge entire balance
                (total - currentPass.balance.amount, currentPass.balance.amount)

            Result(
              newTotal,
              appliedPasses :+ currentPass.copy(
                amountToCharge = entities.MonetaryAmount(newAmountToCharge).some,
              ),
            )
          }
      }

  final case class Result(total: BigDecimal, appliedGiftCardPasses: Seq[entities.GiftCardPassApplied])
}
