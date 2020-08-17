package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class LoyaltyRewardProductRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    loyaltyRewardId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class LoyaltyRewardProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    loyaltyRewardId: Option[UUID],
  ) extends SlickMerchantUpdate[LoyaltyRewardProductRecord] {

  def toRecord: LoyaltyRewardProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert LoyaltyRewardProductUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert LoyaltyRewardProductUpdate without a product id. [$this]")
    require(
      loyaltyRewardId.isDefined,
      s"Impossible to convert LoyaltyRewardProductUpdate without a loyalty reward id. [$this]",
    )
    LoyaltyRewardProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      loyaltyRewardId = loyaltyRewardId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: LoyaltyRewardProductRecord): LoyaltyRewardProductRecord =
    LoyaltyRewardProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      loyaltyRewardId = loyaltyRewardId.getOrElse(record.loyaltyRewardId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
