package io.paytouch.ordering.graphql.datatypes

import java.util.UUID

import sangria.macros.derive._
import sangria.schema._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper

trait CatalogDataType extends StringHelper with MapDataType {
  self: AvailabilitiesDataType with UUIDDataType =>

  implicit private lazy val uuidT = UUIDType

  implicit private lazy val availabilitiesT = AvailabilitiesType

  implicit private lazy val CatalogLocationType =
    deriveObjectType[GraphQLContext, CatalogLocation](TransformFieldNames(_.underscore))

  implicit private lazy val UUIDCatalogLocationEntryType: ObjectType[GraphQLContext, (UUID, CatalogLocation)] =
    deriveMapObjectType(
      keyField = "location_id",
      valueField = "catalog_location",
      name = "UUIDCatalogLocationEntryType",
    )

  lazy val CatalogType: ObjectType[GraphQLContext, Catalog] =
    deriveObjectType[GraphQLContext, Catalog](
      TransformFieldNames(_.underscore),
      ReplaceField(
        "locationOverrides",
        Field(
          "location_overrides",
          OptionType(ListType(UUIDCatalogLocationEntryType)),
          resolve = _.value.locationOverrides.map(_.toSeq),
        ),
      ),
    )
}
