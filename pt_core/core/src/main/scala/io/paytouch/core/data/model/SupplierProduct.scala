package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class SupplierProductRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    supplierId: UUID,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class SupplierProductUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    supplierId: Option[UUID],
  ) extends SlickMerchantUpdate[SupplierProductRecord] {

  def toRecord: SupplierProductRecord = {
    require(merchantId.isDefined, s"Impossible to convert SupplierProductUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert SupplierProductUpdate without a product id. [$this]")
    require(supplierId.isDefined, s"Impossible to convert SupplierProductUpdate without a supplier id. [$this]")
    SupplierProductRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      supplierId = supplierId.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: SupplierProductRecord): SupplierProductRecord =
    SupplierProductRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      supplierId = supplierId.getOrElse(record.supplierId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
