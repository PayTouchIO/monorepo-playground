package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive._
import sangria.schema._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper

trait OrderDataType extends StringHelper {
  self: UUIDDataType
    with EnumDataType
    with MonetaryAmountDataType
    with ZonedDateTimeDataType
    with LocalTimeDataType
    with BigDecimalDataType
    with CurrencyDataType =>

  // TODO this will transform all fields of the Order and child entities. For
  // now that's fine, but we should find a better way to be explicit about what
  // fields we want exposed. Sangria lets you specify fields to exclude, but as
  // this is effectively a public endpoint, it would be better if we were
  // explicit about what fields to include.

  implicit private lazy val acceptanceStatusT = AcceptanceStatusType
  implicit private lazy val uuidT = UUIDType
  implicit private lazy val zonedDateTimeT = ZonedDateTimeType
  implicit private lazy val orderStatusT = OrderStatusType
  implicit private lazy val monetaryT = MonetaryAmountType
  implicit private lazy val paymentStatusT = PaymentStatusType
  implicit private lazy val transactionTypeT = TransactionTypeType
  implicit private lazy val transactionPaymentTypeT = TransactionPaymentTypeType
  implicit private lazy val modifierSetTypeT = ModifierSetTypeType
  implicit private lazy val localTimeT = LocalTimeType

  lazy val OrderBundleOptionType =
    deriveObjectType[GraphQLContext, OrderBundleOption](
      TransformFieldNames(_.underscore),
      ExcludeFields("id", "bundleOptionId"),
    )

  lazy val OrderBundleSetType =
    deriveObjectType[GraphQLContext, OrderBundleSet](
      TransformFieldNames(_.underscore),
      ExcludeFields("id", "bundleSetId", "name"),
      ReplaceField(
        "orderBundleOptions",
        Field(
          "bundle_options",
          ListType(OrderBundleOptionType),
          resolve = _.value.orderBundleOptions,
        ),
      ),
    )

  lazy val OrderBundleType =
    deriveObjectType[GraphQLContext, OrderBundle](
      TransformFieldNames(_.underscore),
      ExcludeFields("id"),
      ReplaceField(
        "orderBundleSets",
        Field(
          "bundle_sets",
          ListType(OrderBundleSetType),
          resolve = _.value.orderBundleSets,
        ),
      ),
    )

  lazy val OrderItemModifierOptionType =
    deriveObjectType[GraphQLContext, OrderItemModifierOption](
      TransformFieldNames(_.underscore),
      RenameField("name", "option_name"),
      RenameField("modifierSetName", "option_type_name"),
    )

  lazy val OrderItemVariantOptionType =
    deriveObjectType[GraphQLContext, OrderItemVariantOption](TransformFieldNames(_.underscore))

  lazy val OrderItemTaxRateType = deriveObjectType[GraphQLContext, OrderItemTaxRate](
    TransformFieldNames(_.underscore),
    RenameField("totalAmount", "amount"),
  )

  lazy val OrderItemType = deriveObjectType[GraphQLContext, OrderItem](
    TransformFieldNames(_.underscore),
    ExcludeFields("productId"),
    ReplaceField(
      "taxRates",
      Field(
        "tax_rates",
        ListType(OrderItemTaxRateType),
        resolve = _.value.taxRates,
      ),
    ),
    ReplaceField(
      "totalPrice",
      Field(
        "total_price",
        OptionType(MonetaryAmountType),
        resolve = _.value.totalPrice,
      ),
    ),
    ReplaceField(
      "variantOptions",
      Field(
        "variant_options",
        ListType(OrderItemVariantOptionType),
        resolve = _.value.variantOptions,
      ),
    ),
    ReplaceField(
      "modifierOptions",
      Field(
        "modifier_options",
        ListType(OrderItemModifierOptionType),
        resolve = _.value.modifierOptions,
      ),
    ),
  )

  lazy val PaymentDetailsType = ObjectType(
    "PaymentDetails",
    fields[GraphQLContext, GenericPaymentDetails](
      Field("amount", BigDecimalType, resolve = _.value.amount),
      Field("currency", CurrencyType, resolve = _.value.currency),
    ),
  )

  lazy val PaymentTransactionType =
    deriveObjectType[GraphQLContext, PaymentTransaction](
      TransformFieldNames(_.underscore),
      ExcludeFields("paymentProcessorV2"),
      ReplaceField(
        "paymentDetails",
        Field(
          "payment_details",
          PaymentDetailsType,
          resolve = _.value.paymentDetails,
        ),
      ),
    )

  lazy val OrderTaxRateType = deriveObjectType[GraphQLContext, OrderTaxRate](
    TransformFieldNames(_.underscore),
    RenameField("totalAmount", "amount"),
  )

  implicit lazy val OnlineOrderAttributeType = deriveObjectType[GraphQLContext, OnlineOrderAttribute](
    TransformFieldNames(_.underscore),
  )

  lazy val OrderType =
    deriveObjectType[GraphQLContext, Order](
      TransformFieldNames(_.underscore),
      ExcludeFields("bundles", "location"),
      ReplaceField(
        "taxRates",
        Field(
          "tax_rates",
          ListType(OrderTaxRateType),
          resolve = _.value.taxRates,
        ),
      ),
      ReplaceField(
        "paymentTransactions",
        Field(
          "payment_transactions",
          ListType(PaymentTransactionType),
          resolve = _.value.paymentTransactions,
        ),
      ),
      ReplaceField(
        "items",
        Field(
          "items",
          ListType(OrderItemType),
          resolve = _.value.items,
        ),
      ),
      ReplaceField(
        "bundles",
        Field(
          "bundles",
          ListType(OrderBundleType),
          resolve = _.value.bundles,
        ),
      ),
      ReplaceField(
        "completedAt",
        Field("completed_at", OptionType(ZonedDateTimeType), resolve = _.value.completedAt),
      ),
    )
}
