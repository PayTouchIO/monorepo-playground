package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.Table
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive._
import sangria.schema._

trait TableDataType extends StringHelper { self: UUIDDataType with OrderDataType =>

  implicit private lazy val uuidT = UUIDType
  implicit private lazy val orderT = OrderType

  lazy val TableType =
    deriveObjectType[GraphQLContext, Table](
      TransformFieldNames(_.underscore),
      AddFields(
        Field(
          "orders",
          ListType(orderT),
          resolve = { ctx =>
            val merchantId = ctx.ctx.merchantId.get
            val tableId = ctx.value.id
            ctx.ctx.services.orderService.findByTableId(tableId, merchantId)
          },
        ),
      ),
    )
}
