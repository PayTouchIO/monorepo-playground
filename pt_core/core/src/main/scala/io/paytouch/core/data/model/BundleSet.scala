package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.ResettableString

final case class BundleSetRecord(
    id: UUID,
    merchantId: UUID,
    bundleId: UUID,
    name: Option[String],
    position: Int,
    minQuantity: Int,
    maxQuantity: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord {
  def productId = bundleId
}

case class BundleSetUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    bundleId: Option[UUID],
    name: ResettableString,
    position: Option[Int],
    minQuantity: Option[Int],
    maxQuantity: Option[Int],
  ) extends SlickMerchantUpdate[BundleSetRecord] {

  def toRecord: BundleSetRecord = {
    require(merchantId.isDefined, s"Impossible to convert BundleSetUpdate without a merchant id. [$this]")
    require(bundleId.isDefined, s"Impossible to convert BundleSetUpdate without a bundle id. [$this]")
    require(position.isDefined, s"Impossible to convert BundleSetUpdate without a position. [$this]")
    require(minQuantity.isDefined, s"Impossible to convert BundleSetUpdate without a minQuantity. [$this]")
    require(maxQuantity.isDefined, s"Impossible to convert BundleSetUpdate without a maxQuantity. [$this]")
    BundleSetRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      bundleId = bundleId.get,
      name = name,
      position = position.get,
      minQuantity = minQuantity.get,
      maxQuantity = maxQuantity.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: BundleSetRecord): BundleSetRecord =
    BundleSetRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      bundleId = bundleId.getOrElse(record.bundleId),
      name = name.getOrElse(record.name),
      position = position.getOrElse(record.position),
      minQuantity = minQuantity.getOrElse(record.minQuantity),
      maxQuantity = maxQuantity.getOrElse(record.maxQuantity),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
