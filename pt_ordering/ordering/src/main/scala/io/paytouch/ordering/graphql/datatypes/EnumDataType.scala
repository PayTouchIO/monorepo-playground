package io.paytouch.ordering.graphql.datatypes

import cats.implicits._

import sangria.schema.{ EnumType, EnumValue }

import enumeratum.EnumEntry

import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.entities.enums._

trait EnumDataType {
  lazy val AcceptanceStatusType: EnumType[AcceptanceStatus] =
    enumType("AcceptanceStatus", "The acceptance status of an order", AcceptanceStatus.values)

  lazy val ArticlePrdType: EnumType[ArticleType] =
    enumType("ArticleType", "The type of an article", ArticleType.values)

  lazy val ArticleScopeType: EnumType[ArticleScope] =
    enumType("ArticleScope", "The scope of an article", ArticleScope.values)

  lazy val DayType: EnumType[Day] =
    enumType("Day", "A day of the week", Day.values)

  lazy val ImageSizeType: EnumType[ImageSize] =
    enumType("ImageSize", "The size of an image", ImageSize.values)

  lazy val ModifierSetTypeType: EnumType[ModifierSetType] =
    enumType("ModifierSetType", "The type of modifier set", ModifierSetType.values)

  lazy val OrderStatusType: EnumType[OrderStatus] =
    enumType("OrderStatus", "The status of an order", OrderStatus.values)

  lazy val PaymentProcessorType: EnumType[PaymentProcessor] =
    enumType("PaymentProcessor", "The merchant's payment processor", PaymentProcessor.values)

  lazy val PaymentMethodTypeDataType: EnumType[PaymentMethodType] =
    enumType("PaymentMethodType", "The store's payment method", PaymentMethodType.values)

  lazy val PaymentStatusType: EnumType[PaymentStatus] =
    enumType("PaymentStatus", "The status of an order", PaymentStatus.values)

  lazy val SetupTypeType: EnumType[SetupType] =
    enumType("SetupType", "The merchant's setup type", SetupType.values)

  lazy val TransactionTypeType: EnumType[TransactionType] =
    enumType("TransactionType", "The payment transaction type", TransactionType.values)

  lazy val TransactionPaymentTypeType: EnumType[TransactionPaymentType] =
    enumType("TransactionPaymentType", "The payment type", TransactionPaymentType.values)

  lazy val UnitPrdType: EnumType[UnitType] =
    enumType("Unit", "The unit of a product", UnitType.values)

  private def enumType[T <: EnumEntry](
      name: String,
      description: String,
      values: Seq[T],
    ): EnumType[T] =
    EnumType[T](
      name,
      description.some,
      values.map { value =>
        EnumValue(
          name = value.entryName,
          value = value,
        )
      }.toList,
    )
}
