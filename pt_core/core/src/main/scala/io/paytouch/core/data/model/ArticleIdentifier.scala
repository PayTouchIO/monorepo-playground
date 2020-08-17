package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleType

final case class ArticleIdentifierRecord(
    id: UUID,
    merchantId: UUID,
    isVariantOfProductId: Option[UUID],
    `type`: ArticleType,
    name: String,
    sku: Option[String],
    upc: Option[String],
    variantOptions: String,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ArticleIdentifierUpdate(id: Option[UUID], merchantId: Option[UUID])
    extends SlickMerchantUpdate[ArticleIdentifierRecord] {

  def updateRecord(record: ArticleIdentifierRecord): ArticleIdentifierRecord = record

  def toRecord: ArticleIdentifierRecord = throw new RuntimeException("Can't update a view")
}
