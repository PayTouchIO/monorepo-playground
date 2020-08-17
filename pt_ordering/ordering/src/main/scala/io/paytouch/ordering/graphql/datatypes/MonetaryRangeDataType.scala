package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }
import sangria.schema._
import io.paytouch.ordering.clients.paytouch.core.entities.MonetaryRange
import io.paytouch.ordering.graphql.GraphQLContext

trait MonetaryRangeDataType {
  self: MonetaryAmountDataType =>
  implicit private lazy val MonetaryAmount = MonetaryAmountType

  lazy val MonetaryRangeType: ObjectType[GraphQLContext, MonetaryRange] =
    deriveObjectType[GraphQLContext, MonetaryRange](
      TransformFieldNames(_.underscore),
    )
}
