package io.paytouch.core.clients.urbanairship

import java.time.ZonedDateTime

import io.paytouch.core.clients.urbanairship.entities.{ FieldValueUpdate, PassUpsertion }
import io.paytouch.core.data.model.enums.GiftCardPassTransactionType
import io.paytouch.core.entities.{ GiftCardPass, LoyaltyMembership }
import io.paytouch.core.entities.enums.PassType

trait PassLoader[A] {
  def upsertionData(a: A): PassUpsertion
  def passIdByPassType(passType: PassType, a: A): String
}

object PassLoader {
  implicit val loyaltyMembershipLoader = new PassLoader[LoyaltyMembership] {
    def upsertionData(loyaltyMembership: LoyaltyMembership) =
      PassUpsertion(
        headers = Map(
          "barcode_value" -> FieldValueUpdate(loyaltyMembership.lookupId),
          "barcodeAltText" -> FieldValueUpdate(loyaltyMembership.lookupId),
        ),
        fields = Map(
          "Points" -> FieldValueUpdate(loyaltyMembership.points.toString),
          "Spend" -> FieldValueUpdate(loyaltyMembership.totalSpend.roundedAmount),
          "Visits" -> FieldValueUpdate(loyaltyMembership.visits.toString),
          "Next Reward" -> FieldValueUpdate(loyaltyMembership.pointsToNextReward.toString),
        ),
      )
    def passIdByPassType(passType: PassType, loyaltyMembership: LoyaltyMembership) =
      s"${passType.entryName}-${loyaltyMembership.id}"
  }

  implicit val giftCardPassLoader = new PassLoader[GiftCardPass] {
    def upsertionData(giftCardPass: GiftCardPass) = {
      val reverseChronoOrdering =
        Ordering.fromLessThan[ZonedDateTime](_ isAfter _)

      val lastSpend =
        giftCardPass
          .transactions
          .flatMap(
            _.sortBy(_.createdAt)(reverseChronoOrdering)
              .find(_.`type` == GiftCardPassTransactionType.Payment)
              .map(_.total),
          )

      val originalValue =
        giftCardPass.originalBalance

      val balance =
        giftCardPass.balance

      PassUpsertion(
        headers = Map(
          "barcode_value" -> FieldValueUpdate(giftCardPass.lookupId),
          "barcodeAltText" -> FieldValueUpdate(giftCardPass.lookupId),
        ),
        fields = Map(
          // iOS-only fields
          "Last Spend" -> FieldValueUpdate(lastSpend.map(_.roundedAmount).getOrElse[BigDecimal](0)),
          "Original Value" -> FieldValueUpdate(originalValue.roundedAmount),
          // Android-only fields
          "Last Spend Text" -> FieldValueUpdate(lastSpend.map(_.show).getOrElse("N/A")),
          "Original Value Text" -> FieldValueUpdate(originalValue.show),
          // Common fields
          "Balance" -> FieldValueUpdate(balance.roundedAmount),
        ),
      )
    }

    def passIdByPassType(passType: PassType, giftCardPass: GiftCardPass) =
      s"gift-${passType.entryName}-${giftCardPass.id}"
  }
}
