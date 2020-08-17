package io.paytouch.ordering.graphql.schema

import sangria.schema._

import io.paytouch.ordering.graphql.datatypes.CustomDataTypes
import io.paytouch.ordering.graphql.GraphQLContext

object StoreSchema extends HasFields {
  private val MerchantSlug: Argument[String] =
    Argument(
      name = "merchant_slug",
      argumentType = StringType,
      description = "the merchant slug",
    )

  private val Slug: Argument[String] =
    Argument(
      name = "slug",
      argumentType = StringType,
      description = "the slug of the store",
    )

  final override val fields: List[Field[GraphQLContext, Unit]] =
    List(
      Field(
        "store",
        OptionType(CustomDataTypes.StoreType),
        arguments = List(MerchantSlug, Slug),
        resolve = { context =>
          UpdateCtx(
            context
              .ctx
              .services
              .storeService
              .findBySlugs(
                merchantSlug = context.arg(MerchantSlug),
                slug = context.arg(Slug),
              ),
          ) { store =>
            context
              .ctx
              .update(
                optMerchantId = store.map(_.merchantId),
                optLocationId = store.map(_.locationId),
              )
          }
        },
      ),
    )
}
