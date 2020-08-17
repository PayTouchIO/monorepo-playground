package io.paytouch.ordering.graphql.schema

import java.util.UUID

import sangria.schema._

import io.paytouch.ordering.graphql.datatypes.CustomDataTypes
import io.paytouch.ordering.graphql.GraphQLContext

object MerchantByIdSchema extends HasFields {
  private val MerchantId: Argument[UUID] =
    Argument(
      name = "id",
      argumentType = CustomDataTypes.UUIDType,
      description = "the merchant id",
    )

  final override val fields: List[Field[GraphQLContext, Unit]] =
    List(
      Field(
        "merchantById",
        OptionType(CustomDataTypes.MerchantType),
        arguments = List(MerchantId),
        resolve = { ctx =>
          val id = ctx arg MerchantId
          val merchantService = ctx.ctx.services.merchantService
          UpdateCtx(merchantService.findByIdWithoutContext(id))(merchant =>
            ctx.ctx.update(optMerchantId = merchant.map(_.id), optLocationId = None),
          )
        },
      ),
    )
}
