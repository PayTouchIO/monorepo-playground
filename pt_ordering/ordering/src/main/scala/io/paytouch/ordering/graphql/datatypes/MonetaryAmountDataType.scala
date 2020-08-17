package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }

trait MonetaryAmountDataType extends StringHelper { self: BigDecimalDataType with CurrencyDataType =>
  implicit private lazy val bigDecimalT = BigDecimalType
  implicit private lazy val currencyT = CurrencyType

  lazy val MonetaryAmountType =
    deriveObjectType[GraphQLContext, MonetaryAmount](TransformFieldNames(_.underscore))
}
