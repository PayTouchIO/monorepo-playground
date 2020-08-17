package io.paytouch.ordering.graphql.schema

import sangria.schema._

import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.datatypes.CustomDataTypes

object MerchantSchema extends HasFields {
  private val MerchantSlug: Argument[String] =
    Argument(
      name = "slug",
      argumentType = StringType,
      description = "the merchant slug",
    )

  final override val fields: List[Field[GraphQLContext, Unit]] =
    List(
      Field(
        "merchant",
        OptionType(CustomDataTypes.MerchantType),
        arguments = List(MerchantSlug),
        resolve = { ctx =>
          val slug = ctx arg MerchantSlug
          val merchantService = ctx.ctx.services.merchantService
          UpdateCtx(merchantService.findBySlug(slug))(merchant =>
            ctx.ctx.update(optMerchantId = merchant.map(_.id), optLocationId = None),
          )
        },
      ),
    )
}
