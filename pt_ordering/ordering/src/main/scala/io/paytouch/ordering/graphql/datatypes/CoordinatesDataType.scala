package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.Coordinates
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }

trait CoordinatesDataType extends MapDataType with StringHelper { self: BigDecimalDataType =>

  implicit private lazy val bigDecimalT = BigDecimalType

  lazy val CoordinatesType = deriveObjectType[GraphQLContext, Coordinates](
    TransformFieldNames(_.underscore),
  )

}
