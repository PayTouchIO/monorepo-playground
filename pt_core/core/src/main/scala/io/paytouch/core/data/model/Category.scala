package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.{ ResettableString, ResettableUUID }

final case class CategoryRecord(
    id: UUID,
    merchantId: UUID,
    catalogId: UUID,
    parentCategoryId: Option[UUID],
    name: String,
    description: Option[String],
    avatarBgColor: Option[String],
    position: Int,
    active: Option[Boolean],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class CategoryUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    catalogId: Option[UUID],
    parentCategoryId: Option[UUID],
    name: Option[String],
    description: ResettableString = None,
    avatarBgColor: ResettableString = None,
    position: Option[Int] = None,
    active: Option[Boolean] = None,
  ) extends SlickMerchantUpdate[CategoryRecord] {

  def toRecord: CategoryRecord = {
    require(merchantId.isDefined, s"Impossible to convert CategoryUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert CategoryUpdate without a name. [$this]")
    require(catalogId.isDefined, s"Impossible to convert CategoryUpdate without a catalog id. [$this]")
    val isMain = parentCategoryId.isEmpty
    CategoryRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      catalogId = catalogId.get,
      parentCategoryId = parentCategoryId,
      name = name.get,
      description = description,
      avatarBgColor = avatarBgColor,
      position = position.getOrElse(0),
      active = if (isMain) None else active.orElse(Some(true)),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CategoryRecord): CategoryRecord = {
    val isMain = parentCategoryId.orElse(record.parentCategoryId).isEmpty
    CategoryRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      catalogId = catalogId.getOrElse(record.catalogId),
      parentCategoryId = parentCategoryId.orElse(record.parentCategoryId),
      name = name.getOrElse(record.name),
      description = description.getOrElse(record.description),
      avatarBgColor = avatarBgColor.getOrElse(record.avatarBgColor),
      position = position.getOrElse(record.position),
      active = if (isMain) None else active.orElse(record.active),
      createdAt = record.createdAt,
      updatedAt = now,
    )
  }
}
