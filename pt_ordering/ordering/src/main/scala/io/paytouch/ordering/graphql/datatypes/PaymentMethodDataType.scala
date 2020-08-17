package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.entities.PaymentMethod
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive._

trait PaymentMethodDataType extends StringHelper { self: EnumDataType =>
  implicit private lazy val paymentMethodTypeT = PaymentMethodTypeDataType

  lazy val PaymentMethodDataType =
    deriveObjectType[GraphQLContext, PaymentMethod](
      TransformFieldNames(_.underscore),
    )

}
