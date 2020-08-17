package io.paytouch.ordering.json

import org.json4s._

import io.paytouch.ordering.clients.google.entities.GStatus
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ OrderType => CoreOrderType, _ }
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCardPassCharge
import io.paytouch.ordering.clients.stripe.entities.{ PaymentIntentStatus => StripePaymentIntentStatus }
import io.paytouch.ordering.data.model
import io.paytouch.ordering.entities
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.json.serializers._
import io.paytouch.ordering.messages.entities.MerchantPayload.OrderingPaymentProcessorConfigUpsertion

trait Json4Formats {
  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats =
    customFormats ++
      CustomSerializers.all ++
      OpaqueTypeSerializers.all ++
      CustomEnumSerializers.all ++
      ResettableSerializers.all +
      CurrencyKeySerializer +
      new EnumKeySerializer(Day) +
      new EnumKeySerializer(ImageSize) +
      new EnumKeySerializer(ImageType) +
      UUIDKeySerializer

  private val customFormats: Formats = new DefaultFormats {
    override val allowNull = false
    override val strictOptionParsing = true
    override val typeHints = ShortTypeHints(
      List(
        classOf[model.EkashuConfig],
        classOf[model.JetdirectConfig],
        classOf[model.WorldpayConfig],
        classOf[model.StripeConfig],
        classOf[model.PaytouchConfig],
        classOf[OrderingPaymentProcessorConfigUpsertion.StripeConfigUpsertion],
        classOf[OrderingPaymentProcessorConfigUpsertion.WorldpayConfigUpsertion],
        classOf[OrderingPaymentProcessorConfigUpsertion.PaytouchConfigUpsertion],
        classOf[GiftCardPassCharge.Failure],
      ),
    )
  }.preservingEmptyValues
}

object CustomSerializers {
  val all = List(
    BigDecimalSerializer,
    BooleanFalseSerializer,
    BooleanSerializer,
    BooleanTrueSerializer,
    CurrencySerializer,
    IntSerializer,
    LocalDateSerializer,
    LocalDateTimeSerializer,
    LocalTimeSerializer,
    PaymentProcessorConfigSerializer,
    PaymentProcessorConfigUpsertionSerializer,
    ProductSerializer,
    UUIDSerializer,
    ZonedDateTimeSerializer,
    ZoneIdSerializer,
  )
}

object CustomEnumSerializers {
  val all = List(
    new EnumSerializer(AcceptanceStatus),
    new EnumSerializer(ArticleScope),
    new EnumSerializer(ArticleType),
    new EnumSerializer(CardTransactionResultType),
    new EnumSerializer(CardTransactionStatusType),
    new EnumSerializer(CardType),
    new EnumSerializer(CartItemType),
    new EnumSerializer(CartStatus),
    new EnumSerializer(CoreOrderType),
    new EnumSerializer(Day),
    new EnumSerializer(ExposedName),
    new EnumSerializer(GStatus),
    new EnumSerializer(ImageSize),
    new EnumSerializer(ImageType),
    new EnumSerializer(ModifierSetType),
    new EnumSerializer(OrderPaymentType),
    new EnumSerializer(OrderSource),
    new EnumSerializer(OrderStatus),
    new EnumSerializer(OrderType),
    new EnumSerializer(PaymentIntentStatus),
    new EnumSerializer(PaymentMethodType),
    new EnumSerializer(PaymentProcessor),
    new EnumSerializer(PaymentStatus),
    new EnumSerializer(PaymentTransactionFeeType),
    new EnumSerializer(SetupType),
    new EnumSerializer(StoreType),
    new EnumSerializer(StripePaymentIntentStatus),
    new EnumSerializer(TransactionPaymentType),
    new EnumSerializer(TransactionType),
    new EnumSerializer(UnitType),
  )
}

object ResettableSerializers {
  val all = List(
    ResettableBigDecimalSerializer,
    ResettableIntSerializer,
    ResettableLocalDateSerializer,
    ResettableLocalTimeSerializer,
    ResettableStringSerializer,
    ResettableUUIDSerializer,
    ResettableZonedDateTimeSerializer,
  )
}
