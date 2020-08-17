package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, AddFields, ExcludeFields, TransformFieldNames }
import sangria.schema._
import com.fasterxml.jackson.core.JsonParser.NumberType

trait LocationDataType extends StringHelper {
  self: AddressDataType
    with AvailabilitiesDataType
    with CoordinatesDataType
    with CurrencyDataType
    with UUIDDataType
    with ZoneIdDataType =>

  implicit private lazy val addressT = AddressType
  implicit private lazy val availabilitiesT = AvailabilitiesType
  implicit private lazy val coordinatesT = CoordinatesType
  implicit private lazy val currencyT = CurrencyType
  implicit private lazy val uuidT = UUIDType
  implicit private lazy val zoneIdT = ZoneIdType

  lazy val LocationType =
    deriveObjectType[GraphQLContext, Location](
      TransformFieldNames(_.underscore),
      ExcludeFields("settings"),
      AddFields(
        Field(
          "default_estimated_prep_time_in_mins",
          OptionType(IntType),
          resolve = { ctx =>
            ctx.value.settings.flatMap(_.onlineOrder.defaultEstimatedPrepTimeInMins)
          },
        ),
      ),
    )
}
