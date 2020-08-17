package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive._
import sangria.schema._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper

trait ModifierSetDataType extends MapDataType with StringHelper {
  self: EnumDataType with ItemLocationDataType with MonetaryAmountDataType with UUIDDataType =>

  implicit private lazy val modifierSetTypeT = ModifierSetTypeType
  implicit private lazy val monetaryAmountT = MonetaryAmountType
  implicit private lazy val uuidT = UUIDType

  implicit private lazy val ModifierOptionType =
    deriveObjectType[GraphQLContext, ModifierOption](TransformFieldNames(_.underscore))

  lazy val ModifierSetType =
    deriveObjectType[GraphQLContext, ModifierSet](
      TransformFieldNames(_.underscore),
      ReplaceField(
        "locationOverrides",
        Field(
          "location_overrides",
          ListType(UUIDItemLocationEntryType),
          resolve = _.value.locationOverrides.toSeq,
        ),
      ),
    )
}
