package io.paytouch.ordering.graphql.schema

import java.util.UUID

import sangria.schema._

import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.datatypes.CustomDataTypes

object LocationSchema extends HasFields {
  private val Id: Argument[UUID] =
    Argument(
      name = "id",
      argumentType = CustomDataTypes.UUIDType,
      description = "the id of the location",
    )

  final override val fields: List[Field[GraphQLContext, Unit]] =
    List(
      Field(
        "location",
        OptionType(CustomDataTypes.LocationType),
        arguments = List(Id),
        resolve = { ctx =>
          val locationId = ctx arg Id
          val locationService = ctx.ctx.services.locationService
          ctx.ctx.findByIdWithContextUpdate(locationService)(locationId)
        },
      ),
    )
}
