package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.{ VariantOption, VariantOptionType, VariantOptionWithType }
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }

trait VariantOptionTypeDataType extends StringHelper { self: UUIDDataType =>

  implicit private lazy val uuidT = UUIDType

  implicit private lazy val VariantOptionType =
    deriveObjectType[GraphQLContext, VariantOption](TransformFieldNames(_.underscore))

  lazy val VariantOptionTypeType =
    deriveObjectType[GraphQLContext, VariantOptionType](TransformFieldNames(_.underscore))

  lazy val VariantOptionWithTypeType =
    deriveObjectType[GraphQLContext, VariantOptionWithType](TransformFieldNames(_.underscore))
}
