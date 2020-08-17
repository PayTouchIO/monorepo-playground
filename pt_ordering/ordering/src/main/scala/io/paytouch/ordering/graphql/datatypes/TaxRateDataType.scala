package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.TaxRate
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, ReplaceField, TransformFieldNames }
import sangria.schema._

trait TaxRateDataType extends MapDataType with StringHelper {
  self: BigDecimalDataType with ItemLocationDataType with UUIDDataType =>

  implicit private lazy val bigDecimalT = BigDecimalType
  implicit private lazy val uuidT = UUIDType

  lazy val TaxRateType = deriveObjectType[GraphQLContext, TaxRate](
    TransformFieldNames(_.underscore),
    ReplaceField(
      "locationOverrides",
      Field("location_overrides", ListType(UUIDItemLocationEntryType), resolve = _.value.locationOverrides.toSeq),
    ),
  )

}
