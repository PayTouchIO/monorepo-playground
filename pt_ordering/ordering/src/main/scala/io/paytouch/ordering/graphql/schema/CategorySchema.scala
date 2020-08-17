package io.paytouch.ordering.graphql.schema

import java.util.UUID

import sangria.schema._

import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.graphql.datatypes.CustomDataTypes
import io.paytouch.ordering.graphql.GraphQLContext

object CategorySchema extends HasFields {
  private val CatalogId: Argument[UUID] =
    Argument(
      name = "catalog_id",
      argumentType = CustomDataTypes.UUIDType,
      description = "the filter catalog id",
    )

  private val LocationId: Argument[UUID] =
    Argument(
      name = "location_id",
      argumentType = CustomDataTypes.UUIDType,
      description = "the filter location id",
    )

  final override val fields: List[Field[GraphQLContext, Unit]] =
    List(
      Field(
        "categories",
        ListType(CustomDataTypes.CategoryType),
        arguments = List(LocationId, CatalogId),
        resolve = { ctx =>
          val catalogId = (ctx arg CatalogId).taggedWith[Catalog]
          val locationId = (ctx arg LocationId).taggedWith[Location]
          val filters = (catalogId, locationId)
          val categoryService = ctx.ctx.services.categoryService
          ctx.ctx.findAllWithContextUpdate(categoryService)(filters)
        },
      ),
    )
}
