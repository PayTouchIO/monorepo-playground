package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.upsertions.VariantArticleUpsertion
import io.paytouch.core.data.model.{
  ArticleRecord,
  ProductLocationTaxRateUpdate,
  ProductLocationUpdate,
  ProductVariantOptionUpdate,
  ArticleUpdate => ArticleUpdateModel,
}
import io.paytouch.core.entities._

trait VariantArticleConversions extends ArticleConversions {

  def toVariantArticleModels(
      parent: ArticleUpdateModel,
      variantArticleUpsertions: Seq[VariantArticleUpdate],
      parentRecord: Option[ArticleRecord],
    ): Seq[ArticleUpdateModel] =
    variantArticleUpsertions.map(toVariantArticleModel(parent, _, parentRecord))

  def toVariantArticleModel(
      parent: ArticleUpdateModel,
      variantArticleUpdate: VariantArticleUpdate,
      parentRecord: Option[ArticleRecord],
    ): ArticleUpdateModel =
    ArticleUpdateModel(
      id = Some(variantArticleUpdate.id),
      merchantId = parent.merchantId.orElse(parentRecord.map(_.merchantId)),
      `type` = Some(ArticleType.Variant),
      isCombo = parent.isCombo,
      name = parent.name.orElse(parentRecord.map(_.name)),
      description = parent.description.getOrElse(parentRecord.flatMap(_.description)),
      brandId = parent.brandId.getOrElse(parentRecord.flatMap(_.brandId)),
      priceAmount = variantArticleUpdate.price,
      costAmount = variantArticleUpdate.cost,
      averageCostAmount = None,
      unit = variantArticleUpdate.unit,
      margin = variantArticleUpdate.margin,
      upc = variantArticleUpdate.upc,
      sku = variantArticleUpdate.sku,
      isVariantOfProductId = parent.id.orElse(parentRecord.map(_.id)),
      hasVariant = Some(false),
      trackInventory = parent.trackInventory.orElse(parentRecord.map(_.trackInventory)),
      active = parent.active.orElse(parentRecord.map(_.active)),
      applyPricingToAllLocations = variantArticleUpdate.applyPricingToAllLocations,
      discountable = variantArticleUpdate.discountable,
      avatarBgColor = variantArticleUpdate.avatarBgColor,
      isService = variantArticleUpdate.isService,
      orderRoutingBar = variantArticleUpdate.orderRoutingBar,
      orderRoutingKitchen = variantArticleUpdate.orderRoutingKitchen,
      orderRoutingEnabled = parent.orderRoutingEnabled.orElse(parentRecord.map(_.orderRoutingEnabled)),
      trackInventoryParts = parent.trackInventoryParts.orElse(parentRecord.map(_.trackInventoryParts)),
      hasParts = None,
      scope = parent.scope,
      deletedAt = parent.deletedAt.orElse(parentRecord.flatMap(_.deletedAt)),
    )

  def toVariantUpsertions(
      products: Seq[ArticleUpdateModel],
      productLocations: Map[UUID, Map[UUID, Option[ProductLocationUpdate]]],
      productLocationTaxRates: Map[UUID, Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]]],
      productVariantOptions: Option[Seq[ProductVariantOptionUpdate]],
    ): Seq[VariantArticleUpsertion] =
    products.flatMap { product =>
      product.id.map { productId =>
        val prodLocs = productLocations.getOrElse(productId, Map.empty)
        val prodLocTaxRates = productLocationTaxRates.getOrElse(productId, Map.empty)
        val prodVarOptions = productVariantOptions.map(_.filter(_.productId.contains(productId)))
        toVariantUpsertion(product, prodLocs, prodLocTaxRates, prodVarOptions)
      }
    }

  private def toVariantUpsertion(
      product: ArticleUpdateModel,
      productLocations: Map[UUID, Option[ProductLocationUpdate]],
      productLocationTaxRates: Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]],
      productVariantOptions: Option[Seq[ProductVariantOptionUpdate]],
    ): VariantArticleUpsertion =
    VariantArticleUpsertion(
      product = product,
      productLocations = productLocations,
      productLocationTaxRates = productLocationTaxRates,
      productVariantOptions = productVariantOptions,
    )
}
