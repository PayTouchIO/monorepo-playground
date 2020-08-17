package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.CategoryRecord

class CategoriesTable(tag: Tag) extends SlickMerchantTable[CategoryRecord](tag, "categories") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def catalogId = column[UUID]("catalog_id")
  def parentCategoryId = column[Option[UUID]]("parent_category_id")
  def name = column[String]("name")
  def description = column[Option[String]]("description")

  def avatarBgColor = column[Option[String]]("avatar_bg_color")

  def position = column[Int]("position")
  def active = column[Option[Boolean]]("active")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      catalogId,
      parentCategoryId,
      name,
      description,
      avatarBgColor,
      position,
      active,
      createdAt,
      updatedAt,
    ).<>(CategoryRecord.tupled, CategoryRecord.unapply)
}
