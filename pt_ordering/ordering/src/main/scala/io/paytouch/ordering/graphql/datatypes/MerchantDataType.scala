package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive._
import sangria.schema._

import cats.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.schema.Fetchers

trait MerchantDataType {
  self: LocationDataType
    with TableDataType
    with EnumDataType
    with OrderDataType
    with StoreDataType
    with UUIDDataType
    with ProductDataType
    with GiftCardDataType =>
  implicit private lazy val locationT = LocationType
  implicit private lazy val orderT = OrderType
  implicit private lazy val productT = ProductType
  implicit private lazy val tableT = TableType
  implicit private lazy val simpleStoreT: ObjectType[GraphQLContext, SimpleStore] = SimpleStoreType
  implicit private lazy val giftCardT: ObjectType[GraphQLContext, GiftCard] =
    GiftCardType
  implicit private lazy val UUIDT = UUIDType
  implicit private lazy val paymentProcessorT = PaymentProcessorType
  implicit private lazy val setupTypeT = SetupTypeType

  private lazy val ekashuConfigType =
    deriveObjectType[GraphQLContext, EkashuConfig](TransformFieldNames(_.underscore))

  private lazy val jetDirectConfigType =
    deriveObjectType[GraphQLContext, JetdirectConfig](TransformFieldNames(_.underscore))

  private lazy val worldpayConfigType =
    deriveObjectType[GraphQLContext, WorldpayConfig](TransformFieldNames(_.underscore))

  implicit private lazy val paymentProcessorConfigType =
    UnionType(
      "PaymentProcessorConfig",
      Some("PaymentProcessorConfig"),
      List(ekashuConfigType, jetDirectConfigType, worldpayConfigType),
    )

  private val LocationId =
    Argument("id", CustomDataTypes.UUIDType, description = "the location id")
  private val TableId =
    Argument("id", CustomDataTypes.UUIDType, description = "the table id")
  private val OrderId =
    Argument("id", CustomDataTypes.UUIDType, description = "the order id")
  private val ProductId =
    Argument("id", CustomDataTypes.UUIDType, description = "the product id")

  lazy val MerchantType = deriveObjectType[GraphQLContext, Merchant](
    TransformFieldNames(_.underscore),
    ReplaceField(
      "paymentProcessorConfig",
      Field(
        "payment_processor_config",
        OptionType(paymentProcessorConfigType),
        // Exclude the configs with no fields in the object and Sangria complains
        resolve = _.value
          .paymentProcessorConfig
          .some
          .filterNot(_.productArity == 0),
      ),
    ),
    AddFields(
      Field(
        "setup_type",
        setupTypeT,
        resolve = { ctx =>
          implicit val ec = ctx.ctx.ec
          val merchantId = ctx.value.id.taggedWith[CoreMerchant]
          DeferredValue(Fetchers.merchants.defer(merchantId)).map(_.setupType)
        },
      ),
      Field(
        "location",
        OptionType(locationT),
        arguments = LocationId :: Nil,
        resolve = { ctx =>
          val merchantId = ctx.value.id
          val locationId = ctx arg LocationId
          ctx.ctx.services.locationService.findByMerchantIdAndId(merchantId, locationId)
        },
      ),
      Field(
        "locations",
        ListType(locationT),
        resolve = { ctx =>
          val merchantId = ctx.value.id
          ctx.ctx.services.locationService.findAll(merchantId)
        },
        deprecationReason = Some("Field is deprecated. Use `stores.location` instead."),
      ),
      Field(
        "order",
        OptionType(orderT),
        arguments = OrderId :: Nil,
        resolve = { ctx =>
          val merchantId = ctx.value.id
          val orderId = ctx arg OrderId
          ctx.ctx.services.orderService.findById(orderId, merchantId)
        },
      ),
      Field(
        "product",
        OptionType(productT),
        arguments = ProductId :: Nil,
        resolve = Projector { (ctx, projection) =>
          val merchantId = ctx.value.id
          val productId = ctx arg ProductId
          val productExpansions = ProductExpansions.fromProjectionToExpansion(ProductExpansions.empty, projection)
          ctx.ctx.services.productService.findById(productId, merchantId, productExpansions)
        },
      ),
      Field(
        "stores",
        ListType(simpleStoreT),
        resolve = { ctx =>
          val merchantId = ctx.value.id
          ctx.ctx.services.storeService.findAllSimpleByMerchantId(merchantId)
        },
      ),
      Field(
        "table",
        OptionType(tableT),
        arguments = TableId :: Nil,
        resolve = { ctx =>
          val merchantId = ctx.value.id
          val tableId = ctx arg TableId
          ctx.ctx.services.tableService.findByMerchantIdAndId(merchantId, tableId)
        },
      ),
      Field(
        "gift_card",
        OptionType(giftCardT),
        resolve = { ctx =>
          implicit val ec = ctx.ctx.ec
          val merchantId = ctx.value.id
          ctx
            .ctx
            .services
            .giftCardService
            .findAll((), merchantId)
            .map(_.headOption)
        },
      ),
    ),
  )
}
