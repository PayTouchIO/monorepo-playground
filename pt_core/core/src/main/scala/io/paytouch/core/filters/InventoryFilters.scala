package io.paytouch.core.filters

import java.util.UUID

import cats.implicits._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ArticleTypeAlias }

final case class InventoryFilters(
    categoryIds: Option[Seq[UUID]] = None,
    locationIds: Option[Seq[UUID]] = None,
    lowInventory: Option[Boolean] = None,
    query: Option[String] = None,
    supplierId: Option[UUID] = None,
    isCombo: Option[Boolean] = None,
    articleTypes: Option[Seq[ArticleType]] = None,
    articleScope: Option[ArticleScope] = None,
  ) extends BaseFilters

object InventoryFilters {
  def forArticlesList(
      categoryId: Option[UUID],
      categoryIds: Option[Seq[UUID]],
      locationIds: Option[Seq[UUID]],
      lowInventory: Option[Boolean],
      query: Option[String],
      supplierId: Option[UUID],
      isCombo: Option[Boolean],
      scope: Option[ArticleScope],
      alias: Option[ArticleTypeAlias],
      aliases: Option[Seq[ArticleTypeAlias]],
    ): InventoryFilters = {
    val allCategoryIds: Option[Seq[UUID]] = categoryId.map(List(_)) |+| categoryIds.map(_.toList)
    val articleTypes = ArticleTypeAlias.toArticleTypes(alias, aliases)

    apply(
      allCategoryIds,
      locationIds,
      lowInventory,
      query,
      supplierId,
      isCombo,
      Some(articleTypes),
      scope,
    )
  }

  def forProductsList(
      categoryId: Option[UUID],
      categoryIds: Option[Seq[UUID]],
      locationIds: Option[Seq[UUID]],
      lowInventory: Option[Boolean],
      query: Option[String],
      supplierId: Option[UUID],
      isCombo: Option[Boolean],
    ): InventoryFilters = {
    val scope = ArticleScope.Product
    forArticlesList(
      categoryId,
      categoryIds,
      locationIds,
      lowInventory,
      query,
      supplierId,
      isCombo,
      Some(scope),
      None,
      None,
    )
  }
}
