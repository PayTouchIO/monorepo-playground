package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.Availability
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }

trait AvailabilityDataType extends StringHelper { self: LocalTimeDataType =>
  implicit private lazy val localTimeT = LocalTimeType
  lazy val AvailabilityType = deriveObjectType[GraphQLContext, Availability](TransformFieldNames(_.underscore))

}
