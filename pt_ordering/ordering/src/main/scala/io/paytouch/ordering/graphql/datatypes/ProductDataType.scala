package io.paytouch.ordering.graphql.datatypes

import java.util.UUID

import io.paytouch.ordering._
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.schema.Fetchers
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, AddFields, ReplaceField, TransformFieldNames }
import sangria.schema.{ Field, ListType, ObjectType, OptionType }
import sangria.schema._

trait ProductDataType extends MapDataType with StringHelper {
  self: EnumDataType
    with BigDecimalDataType
    with BundleSetDataType
    with ImageUrlsDataType
    with ModifierSetDataType
    with MonetaryAmountDataType
    with MonetaryRangeDataType
    with CategoryOptionDataType
    with TaxRateDataType
    with UUIDDataType
    with VariantOptionTypeDataType =>

  implicit private lazy val articleTypeT = ArticlePrdType
  implicit private lazy val articleScopeT = ArticleScopeType
  implicit private lazy val bigDecimalT = BigDecimalType
  implicit private lazy val bundleSetT = BundleSetType
  implicit private lazy val imageUrlsT = ImageUrlsType
  implicit private lazy val modifierSetT = ModifierSetType
  implicit private lazy val monetaryT = MonetaryAmountType
  implicit private lazy val monetaryRangeT = MonetaryRangeType
  implicit private lazy val catalogCategoryOptionT = CatalogCategoryOptionDataType
  implicit private lazy val taxRateT = TaxRateType
  implicit private lazy val unitT = UnitPrdType
  implicit private lazy val uuidT = UUIDType
  implicit private lazy val variantOptionWithTypeT = VariantOptionWithTypeType
  implicit private lazy val variantOptionTypeT = VariantOptionTypeType

  implicit private lazy val CategoryPositionType =
    deriveObjectType[GraphQLContext, CategoryPosition](TransformFieldNames(_.underscore))

  implicit private lazy val ModifierPositionType =
    deriveObjectType[GraphQLContext, ModifierPosition](TransformFieldNames(_.underscore))

  implicit private lazy val StockType =
    deriveObjectType[GraphQLContext, Stock](TransformFieldNames(_.underscore))

  implicit private lazy val ProductLocationType =
    deriveObjectType[GraphQLContext, ProductLocation](TransformFieldNames(_.underscore))

  implicit private lazy val UUIDProductLocationEntryType: ObjectType[GraphQLContext, (UUID, ProductLocation)] =
    deriveMapObjectType(
      keyField = "location_id",
      valueField = "product_location",
      name = "UUIDProductLocationEntryType",
    )

  lazy val ProductType: ObjectType[GraphQLContext, Product] = deriveObjectType[GraphQLContext, Product](
    TransformFieldNames(_.underscore),
    ReplaceField(
      "locationOverrides",
      Field("location_overrides", ListType(UUIDProductLocationEntryType), resolve = _.value.locationOverrides.toSeq),
    ),
    ReplaceField(
      "variantProducts",
      Field("variant_products", OptionType(ListType(ProductType)), resolve = _.value.variantProducts),
    ),
    ReplaceField("bundleSets", Field("bundle_sets", ListType(bundleSetT), resolve = _.value.bundleSets)),
    ReplaceField(
      "modifiers",
      Field(
        "modifiers",
        ListType(modifierSetT),
        resolve = { ctx =>
          implicit val ec = ctx.ctx.ec
          val merchantId = ctx.ctx.merchantId.taggedWith[Merchant]
          val modifierSetIds = ctx.value.modifierIds.getOrElse(Seq.empty).map { modifierSetId =>
            (modifierSetId.taggedWith[ModifierSet], merchantId)
          }
          val modifierSetsByRelId = Fetchers.modifiers.deferSeq(modifierSetIds)
          DeferredValue(modifierSetsByRelId).map(_.map(_._2))
        },
      ),
    ),
    AddFields(
      Field(
        "template",
        OptionType(ProductType),
        resolve = { ctx =>
          ctx.value.isVariantOfProductId match {
            case Some(isVariantOfProductIdValue) =>
              implicit val ec = ctx.ctx.ec
              val isVariantOfProductId = isVariantOfProductIdValue.taggedWith[Product]
              val merchantId = ctx.ctx.merchantId.taggedWith[Merchant]
              val relId = (isVariantOfProductId, merchantId)
              val productByProductId = Fetchers.products.defer(relId)
              DeferredValue(productByProductId).map { case (_, product) => Some(product) }
            case None => Value(None)
          }
        },
      ),
    ),
  )

}
