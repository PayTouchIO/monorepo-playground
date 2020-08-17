package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.Availability
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.graphql.GraphQLContext
import sangria.schema._

trait AvailabilitiesDataType { self: AvailabilityDataType with EnumDataType =>

  private type Availabilities = Map[Day, Seq[Availability]]

  implicit private lazy val availabilityT = AvailabilityType
  implicit private lazy val dayT = DayType

  lazy val AvailabilitiesType = {
    val availabilityFields: Seq[Field[GraphQLContext, Availabilities]] =
      Day.values.map { day =>
        Field(
          day.entryName,
          ListType(AvailabilityType),
          resolve = { ctx: Context[GraphQLContext, Availabilities] =>
            ctx.value.getOrElse(day, Seq.empty)
          },
        )
      }
    ObjectType("Availabilties", fields(availabilityFields: _*))
  }
}
