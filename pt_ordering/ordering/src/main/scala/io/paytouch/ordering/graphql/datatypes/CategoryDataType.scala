package io.paytouch.ordering.graphql.datatypes

import cats.implicits._
import java.util.UUID

import io.paytouch.implicits._
import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.schema.Fetchers
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, AddFields, ReplaceField, TransformFieldNames }
import sangria.schema._
import io.paytouch.ordering.services.ProductService

trait CategoryDataType extends MapDataType with StringHelper {
  self: AvailabilitiesDataType with ImageUrlsDataType with ProductDataType with UUIDDataType =>

  implicit private lazy val availabilitiesT = AvailabilitiesType
  implicit private lazy val imageUrlsT = ImageUrlsType
  implicit private lazy val productT = ProductType
  implicit private lazy val uuidT = UUIDType

  implicit private lazy val CategoryLocationType =
    deriveObjectType[GraphQLContext, CategoryLocation](TransformFieldNames(_.underscore))

  implicit private lazy val UUIDCategoryLocationEntryType: ObjectType[GraphQLContext, (UUID, CategoryLocation)] =
    deriveMapObjectType(
      keyField = "location_id",
      valueField = "category_location",
      name = "UUIDCategoryLocationEntryType",
    )

  lazy val CategoryType: ObjectType[GraphQLContext, Category] =
    deriveObjectType[GraphQLContext, Category](
      TransformFieldNames(_.underscore),
      ReplaceField("subcategories", Field("subcategories", ListType(CategoryType), resolve = _.value.subcategories)),
      ReplaceField(
        "locationOverrides",
        Field(
          "location_overrides",
          OptionType(ListType(UUIDCategoryLocationEntryType)),
          resolve = _.value.locationOverrides.map(_.toSeq),
        ),
      ),
      AddFields(
        Field(
          "products",
          ListType(productT),
          resolve = Projector { (ctx, projection) =>
            implicit val ec = ctx.ctx.ec
            val categoryId = ctx.value.id.taggedWith[Category]
            val catalogId = ctx.value.catalogId.taggedWith[Catalog]
            val locationId = ctx.ctx.locationId.taggedWith[Location]
            val merchantId = ctx.ctx.merchantId.taggedWith[Merchant]
            // We always need category data to group by RelId, so we start the fold with that
            val productExpansions =
              ProductExpansions.fromProjectionToExpansion(ProductExpansions.empty.withCategoryData, projection)
            val relId = ProductService.RelId(categoryId, catalogId, locationId, merchantId, productExpansions)
            val productsWithCategoryId = Fetchers.categoryProducts.defer(relId)
            DeferredValue(productsWithCategoryId).map {
              case (_, products) => products
            }
          },
        ),
      ),
    )
}
