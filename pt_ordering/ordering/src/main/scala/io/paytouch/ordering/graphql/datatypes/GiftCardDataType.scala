package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive._
import sangria.macros.derive.TransformFieldNames
import sangria.schema._

import io.paytouch.ordering.clients.paytouch.core.entities.GiftCard
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper

trait GiftCardDataType {
  self: ImageUrlsDataType
    with MonetaryAmountDataType
    with ProductDataType
    with UUIDDataType
    with ZonedDateTimeDataType =>
  implicit private lazy val ImageUrls = ImageUrlsType
  implicit private lazy val MonetaryAmount = MonetaryAmountType
  implicit private lazy val Product = ProductType
  implicit private lazy val UUID = UUIDType
  implicit private lazy val ZonedDateTime = ZonedDateTimeType

  lazy val GiftCardType: ObjectType[GraphQLContext, GiftCard] =
    deriveObjectType[GraphQLContext, GiftCard](
      TransformFieldNames(_.underscore),
    )
}
