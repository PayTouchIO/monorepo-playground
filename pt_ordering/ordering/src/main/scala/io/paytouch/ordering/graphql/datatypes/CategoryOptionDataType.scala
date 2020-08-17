package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.CategoryOption
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }

trait CategoryOptionDataType extends StringHelper { self: UUIDDataType =>

  implicit private lazy val uuidT = UUIDType

  lazy val CatalogCategoryOptionDataType =
    deriveObjectType[GraphQLContext, CategoryOption](TransformFieldNames(_.underscore))

}
