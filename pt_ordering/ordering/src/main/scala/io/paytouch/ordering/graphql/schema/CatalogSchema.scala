package io.paytouch.ordering.graphql.schema

import java.util.UUID

import sangria.schema._

import io.paytouch.ordering.graphql.datatypes.CustomDataTypes
import io.paytouch.ordering.graphql.GraphQLContext

object CatalogSchema extends HasFields {
  private val Id: Argument[UUID] =
    Argument(
      name = "id",
      argumentType = CustomDataTypes.UUIDType,
      description = "the id of the catalog",
    )

  final override val fields: List[Field[GraphQLContext, Unit]] =
    List(
      Field(
        "catalog",
        OptionType(CustomDataTypes.CatalogType),
        arguments = List(Id),
        resolve = { ctx =>
          val catalogId = ctx arg Id
          val catalogService = ctx.ctx.services.catalogService
          ctx.ctx.findByIdWithContextUpdate(catalogService)(catalogId)
        },
      ),
    )
}
