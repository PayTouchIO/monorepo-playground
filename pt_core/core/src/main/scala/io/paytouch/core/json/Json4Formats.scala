package io.paytouch.core.json

import org.json4s._

import io.paytouch.core._
import io.paytouch.core.barcodes.entities.enum.BarcodeFormat
import io.paytouch.core.clients.urbanairship.entities.enums.ProjectType
import io.paytouch.core.data._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.json.serializers._
import io.paytouch.core.messages.entities.MerchantPayload.OrderingPaymentProcessorConfigUpsertion
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.ServiceConfigurations
import io.paytouch.core.services._

trait Json4Formats {
  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats =
    CustomValidatedSerializers.including(allTheOtherSerializers)

  private lazy val allTheOtherSerializers: Formats =
    customFormats ++
      CustomSerializers.all ++
      OpaqueTypeSerializers.all ++
      CustomEnumSerializers.all ++
      ResettableSerializers.all +
      CurrencyKeySerializer +
      DayKeySerializer +
      UUIDKeySerializer +
      new EnumKeySerializer(ImageUploadType) +
      new EnumKeySerializer(KitchenType) +
      TransactionPaymentTypeKeySerializer +
      MerchantSetupStepsKeySerializer

  private val customFormats: Formats = new DefaultFormats {
    override val allowNull = false
    override val strictOptionParsing = ServiceConfigurations.isTestEnv
    override val typeHints = ShortTypeHints(
      List(
        classOf[model.PaymentProcessorConfig.Creditcall],
        classOf[entities.PaymentProcessorConfig.Creditcall],
        classOf[model.PaymentProcessorConfig.Jetpay],
        classOf[entities.PaymentProcessorConfig.Jetpay],
        classOf[model.PaymentProcessorConfig.Paytouch],
        classOf[entities.PaymentProcessorConfig.Paytouch],
        classOf[model.PaymentProcessorConfig.Stripe],
        classOf[entities.PaymentProcessorConfig.Stripe],
        classOf[model.PaymentProcessorConfig.Worldpay],
        classOf[entities.PaymentProcessorConfig.Worldpay],
        classOf[OrderingPaymentProcessorConfigUpsertion.StripeConfigUpsertion],
        classOf[OrderingPaymentProcessorConfigUpsertion.WorldpayConfigUpsertion],
        classOf[GiftCardPassCharge.Failure],
      ),
    )
  }.preservingEmptyValues
}

object CustomSerializers {
  val all = List(
    BigDecimalSerializer,
    BooleanSerializer,
    CurrencySerializer,
    IntSerializer,
    LocalDateSerializer,
    LocalDateTimeSerializer,
    LocalTimeSerializer,
    LocalTimeSerializer,
    MonetaryAmountSerializer,
    UUIDSerializer,
    ZonedDateTimeSerializer,
    ZoneIdSerializer,
  )
}

object CustomValidatedSerializers {
  def including(allTheOtherSerializers: Formats): Formats =
    allTheOtherSerializers ++
      List(
        AddressSerializer.Address(allTheOtherSerializers),
        AddressSerializer.AddressImproved(allTheOtherSerializers),
        AddressSerializer.AddressImprovedSync(allTheOtherSerializers),
        AddressSerializer.AddressImprovedUpsertion(allTheOtherSerializers),
        AddressSerializer.AddressSync(allTheOtherSerializers),
        AddressSerializer.AddressUpsertion(allTheOtherSerializers),
      )
}

object CustomEnumSerializers {
  val all = List(
    new EnumSerializer(AcceptanceStatus),
    new EnumSerializer(ArticleScope),
    new EnumSerializer(ArticleType),
    new EnumSerializer(BarcodeFormat),
    new EnumSerializer(BusinessType),
    new EnumSerializer(CancellationStatus),
    new EnumSerializer(CardTransactionResultType),
    new EnumSerializer(CardTransactionStatusType),
    new EnumSerializer(CardType),
    new EnumSerializer(CashDrawerActivityType),
    new EnumSerializer(CashDrawerManagementMode),
    new EnumSerializer(CashDrawerStatus),
    new EnumSerializer(CatalogType),
    new EnumSerializer(ChangeReason),
    new EnumSerializer(CommentType),
    new EnumSerializer(ContextSource),
    new EnumSerializer(CustomerSource),
    new EnumSerializer(DeliveryProvider),
    new EnumSerializer(DiscountType),
    new EnumSerializer(ExportStatus),
    new EnumSerializer(ExposedName),
    new EnumSerializer(FrequencyInterval),
    new EnumSerializer(FulfillmentStatus),
    new EnumSerializer(GiftCardPassTransactionType),
    new EnumSerializer(HandledVia),
    new EnumSerializer(ImageUploadType),
    new EnumSerializer(ImportStatus),
    new EnumSerializer(ImportType),
    new EnumSerializer(InventoryCountStatus),
    new EnumSerializer(KitchenType),
    new EnumSerializer(LoadingStatus),
    new EnumSerializer(LoginSource),
    new EnumSerializer(LoyaltyPointsMode),
    new EnumSerializer(LoyaltyProgramType),
    new EnumSerializer(MerchantMode),
    new EnumSerializer(MerchantSetupStatus),
    new EnumSerializer(MerchantSetupSteps),
    new EnumSerializer(ModifierSetType),
    new EnumSerializer(OrderPaymentType),
    new EnumSerializer(OrderRoutingStatus),
    new EnumSerializer(OrderStatus),
    new EnumSerializer(OrderType),
    new EnumSerializer(PaymentProcessor),
    new EnumSerializer(PaymentStatus),
    new EnumSerializer(PaymentTransactionFeeType),
    new EnumSerializer(PaySchedule),
    new EnumSerializer(ProjectType),
    new EnumSerializer(PurchaseOrderPaymentStatus),
    new EnumSerializer(QuantityChangeReason),
    new EnumSerializer(ReceivingObjectStatus),
    new EnumSerializer(ReceivingOrderObjectType),
    new EnumSerializer(ReceivingOrderPaymentMethod),
    new EnumSerializer(ReceivingOrderPaymentStatus),
    new EnumSerializer(ReceivingOrderStatus),
    new EnumSerializer(ReportInterval),
    new EnumSerializer(RestaurantType),
    new EnumSerializer(ReturnOrderReason),
    new EnumSerializer(ReturnOrderStatus),
    new EnumSerializer(RewardRedemptionStatus),
    new EnumSerializer(RewardRedemptionType),
    new EnumSerializer(RewardType),
    new EnumSerializer(ScopeType),
    new EnumSerializer(SetupType),
    new EnumSerializer(ShiftStatus),
    new EnumSerializer(Source),
    new EnumSerializer(StoreType),
    new EnumSerializer(TicketStatus),
    new EnumSerializer(TimeCardStatus),
    new EnumSerializer(TimeOffType),
    new EnumSerializer(TipsHandlingMode),
    new EnumSerializer(TrackableAction),
    new EnumSerializer(TransactionPaymentProcessor),
    new EnumSerializer(TransactionPaymentType),
    new EnumSerializer(TransactionType),
    new EnumSerializer(TransferOrderType),
    new EnumSerializer(UnitType),
  )
}

object ResettableSerializers {
  val all = List(
    ResettableBigDecimalSerializer,
    ResettableBillingDetailsSerializer,
    ResettableIntSerializer,
    ResettableLocalDateSerializer,
    ResettableLocalTimeSerializer,
    ResettableSeatingSerializer,
    ResettableStringSerializer,
    ResettableUUIDSerializer,
    ResettableZonedDateTimeSerializer,
  )
}
