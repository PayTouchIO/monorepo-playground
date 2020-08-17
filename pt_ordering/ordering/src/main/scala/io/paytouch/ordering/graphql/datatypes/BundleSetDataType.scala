package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.BundleSet
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, ReplaceField, TransformFieldNames }
import sangria.schema.{ Field, ListType }

trait BundleSetDataType extends StringHelper { self: BigDecimalDataType with UUIDDataType with BundleOptionDataType =>

  implicit private lazy val uuidT = UUIDType
  implicit private lazy val bigDecimalT = BigDecimalType
  implicit private lazy val bundleOptionT = BundleOptionDataType

  lazy val BundleSetType =
    deriveObjectType[GraphQLContext, BundleSet](
      TransformFieldNames(_.underscore),
      ReplaceField("options", Field("options", ListType(BundleOptionDataType), resolve = _.value.options)),
    )

}
