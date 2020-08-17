package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class BundleOptionRecord(
    id: UUID,
    merchantId: UUID,
    bundleSetId: UUID,
    articleId: UUID,
    priceAdjustment: BigDecimal,
    position: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class BundleOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    bundleSetId: Option[UUID],
    articleId: Option[UUID],
    priceAdjustment: Option[BigDecimal],
    position: Option[Int],
  ) extends SlickMerchantUpdate[BundleOptionRecord] {

  def toRecord: BundleOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert BundleOptionUpdate without a merchant id. [$this]")
    require(bundleSetId.isDefined, s"Impossible to convert BundleOptionUpdate without a bundle set id. [$this]")
    require(articleId.isDefined, s"Impossible to convert BundleOptionUpdate without a bundle set id. [$this]")
    require(priceAdjustment.isDefined, s"Impossible to convert BundleOptionUpdate without a price amount. [$this]")
    require(position.isDefined, s"Impossible to convert BundleOptionUpdate without a position. [$this]")
    BundleOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      bundleSetId = bundleSetId.get,
      articleId = articleId.get,
      priceAdjustment = priceAdjustment.get,
      position = position.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: BundleOptionRecord): BundleOptionRecord =
    BundleOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      bundleSetId = bundleSetId.getOrElse(record.bundleSetId),
      articleId = articleId.getOrElse(record.articleId),
      priceAdjustment = priceAdjustment.getOrElse(record.priceAdjustment),
      position = position.getOrElse(record.position),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
