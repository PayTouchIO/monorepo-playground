package io.paytouch.core.filters

import java.time.ZonedDateTime
import java.util.UUID

import cats.implicits._
import io.paytouch._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ArticleTypeAlias }

final case class ArticleFilters(
    categoryIds: Option[Seq[UUID]] = None,
    locationIds: Option[Seq[UUID]] = None,
    modifierSetId: Option[UUID] = None,
    supplierId: Option[UUID] = None,
    loyaltyRewardId: Option[UUID] = None,
    query: Option[String] = None,
    updatedSince: Option[ZonedDateTime] = None,
    lowInventory: Option[Boolean] = None,
    isCombo: Option[Boolean] = None,
    scope: Option[ArticleScope] = None,
    articleTypes: Option[Seq[ArticleType]] = None,
    ids: Option[Seq[UUID]] = None,
    catalogIds: Option[Seq[CatalogIdPostgres]] = None,
  ) extends BaseFilters

object ArticleFilters {

  def forList(
      systemCategoryId: Option[UUID],
      systemCategoryIds: Option[Seq[UUID]],
      locationIds: Option[Seq[UUID]],
      modifierSetId: Option[UUID],
      supplierId: Option[UUID],
      loyaltyRewardId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
      lowInventory: Option[Boolean],
      isCombo: Option[Boolean],
      scope: Option[ArticleScope],
      alias: Option[ArticleTypeAlias],
      aliases: Option[Seq[ArticleTypeAlias]],
      catalogCategoryIds: Option[Seq[UUID]],
      ids: Option[Seq[UUID]] = None,
      catalogIds: Option[Seq[CatalogIdPostgres]] = None,
    ): ArticleFilters = {
    val categoryIds: Option[Seq[UUID]] =
      systemCategoryId.map(List(_)) |+|
        systemCategoryIds.map(_.toList) |+|
        catalogCategoryIds.map(_.toList)

    apply(
      categoryIds = categoryIds,
      locationIds,
      modifierSetId,
      supplierId,
      loyaltyRewardId,
      query,
      updatedSince,
      lowInventory,
      isCombo,
      scope,
      ArticleTypeAlias.toArticleTypes(alias, aliases).some,
      ids,
      catalogIds,
    )
  }
}
