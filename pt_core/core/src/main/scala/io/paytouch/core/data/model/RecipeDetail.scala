package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class RecipeDetailRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    makesQuantity: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord

case class RecipeDetailUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    makesQuantity: Option[BigDecimal],
  ) extends SlickProductUpdate[RecipeDetailRecord] {

  def toRecord: RecipeDetailRecord = {
    require(merchantId.isDefined, s"Impossible to convert RecipeDetailUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert RecipeDetailUpdate without a product id. [$this]")
    RecipeDetailRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      makesQuantity = makesQuantity.getOrElse(0.0),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: RecipeDetailRecord): RecipeDetailRecord =
    RecipeDetailRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      makesQuantity = makesQuantity.getOrElse(record.makesQuantity),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
