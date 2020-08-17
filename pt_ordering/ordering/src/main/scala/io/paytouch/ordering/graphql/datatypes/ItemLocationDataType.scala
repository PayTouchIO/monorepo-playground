package io.paytouch.ordering.graphql.datatypes

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.ItemLocation
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }
import sangria.schema.ObjectType

trait ItemLocationDataType extends MapDataType with StringHelper { self: UUIDDataType =>

  implicit private lazy val uuidT = UUIDType

  implicit private lazy val ItemLocationType =
    deriveObjectType[GraphQLContext, ItemLocation](TransformFieldNames(_.underscore))

  lazy val UUIDItemLocationEntryType: ObjectType[GraphQLContext, (UUID, ItemLocation)] =
    deriveMapObjectType(keyField = "location_id", valueField = "item_location", name = "UUIDItemLocationEntryType")
}
