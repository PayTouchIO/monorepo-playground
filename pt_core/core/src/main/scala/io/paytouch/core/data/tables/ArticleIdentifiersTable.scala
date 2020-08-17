package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ArticleIdentifierRecord, ArticleIdentifierUpdate }
import io.paytouch.core.data.model.enums.ArticleType

class ArticleIdentifiersTable(tag: Tag)
    extends SlickMerchantTable[ArticleIdentifierRecord](tag, "article_identifiers") {

  def id = column[UUID]("id")
  def merchantId = column[UUID]("merchant_id")
  def isVariantOfProductId = column[Option[UUID]]("is_variant_of_product_id")
  def `type` = column[ArticleType]("type")
  def name = column[String]("name")
  def sku = column[Option[String]]("sku")
  def upc = column[Option[String]]("upc")
  def variantOptions = column[String]("variant_options")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      isVariantOfProductId,
      `type`,
      name,
      sku,
      upc,
      variantOptions,
      createdAt,
      updatedAt,
    ).<>(ArticleIdentifierRecord.tupled, ArticleIdentifierRecord.unapply)
}
