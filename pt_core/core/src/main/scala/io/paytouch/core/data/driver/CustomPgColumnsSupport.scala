package io.paytouch.core.data.driver

import java.util.{ Currency, UUID }

import scala.reflect.ClassTag

import enumeratum._

import io.paytouch.core.data.model.enums.{ ReceivingOrderPaymentMethod, _ }
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.ScopeKey
import io.paytouch.core.reports.data.model.enums.ExportStatus

trait CustomPgColumnsSupport { self: CustomPostgresDriver =>
  trait CustomImplicits { self: API =>
    implicit val currencyMapper =
      MappedColumnType
        .base[Currency, String](
          _.getCurrencyCode,
          Currency.getInstance,
        )

    implicit val scopeKeyMapper =
      MappedColumnType
        .base[ScopeKey, String](
          _.toDb,
          ScopeKey.apply,
        )

    implicit val uuidsMapper = {
      val sep = ","

      MappedColumnType
        .base[Seq[UUID], String](
          _.mkString(sep),
          _.split(sep)
            .toSeq
            .map(UUID.fromString),
        )
    }

    private def enumMapper[T <: EnumEntry: ClassTag](f: String => T) =
      MappedColumnType.base[T, String](_.entryName, f)

    implicit val acceptanceStatusMapper = enumMapper(AcceptanceStatus.withName)
    implicit val articleScopeMapper = enumMapper(ArticleScope.withName)
    implicit val articleTypeMapper = enumMapper(ArticleType.withName)
    implicit val businessTypeMapper = enumMapper(BusinessType.withName)
    implicit val cancellationStatusMapper = enumMapper(CancellationStatus.withName)
    implicit val cashDrawerActivityMapper = enumMapper(CashDrawerActivityType.withName)
    implicit val cashDrawerManagementModeMapper = enumMapper(CashDrawerManagementMode.withName)
    implicit val cashDrawerStatusMapper = enumMapper(CashDrawerStatus.withName)
    implicit val catalogTypeMapper = enumMapper(CatalogType.withName)
    implicit val changeReasonMapper = enumMapper(ChangeReason.withName)
    implicit val commentTypeMapper = enumMapper(CommentType.withName)
    implicit val customerSourceMapper = enumMapper(CustomerSource.withName)
    implicit val deliveryProviderMapper = enumMapper(DeliveryProvider.withName)
    implicit val discountTypeMapper = enumMapper(DiscountType.withName)
    implicit val exportStatusMapper = enumMapper(ExportStatus.withName)
    implicit val exposedNameMapper = enumMapper(ExposedName.withName)
    implicit val frequencyIntervalMapper = enumMapper(FrequencyInterval.withName)
    implicit val fulfillmentStatusMapper = enumMapper(FulfillmentStatus.withName)
    implicit val handledViaMapper = enumMapper(HandledVia.withName)
    implicit val imageUploadTypeMapper = enumMapper(ImageUploadType.withName)
    implicit val importStatusMapper = enumMapper(ImportStatus.withName)
    implicit val importTypeMapper = enumMapper(ImportType.withName)
    implicit val inventoryCountStatusMapper = enumMapper(InventoryCountStatus.withName)
    implicit val itemTypeMapper = enumMapper(AvailabilityItemType.withName)
    implicit val kitchenTypeMapper = enumMapper(KitchenType.withName)
    implicit val loadingStatusMapper = enumMapper(LoadingStatus.withName)
    implicit val loginSourceMapper = enumMapper(LoginSource.withName)
    implicit val loyaltyPointsHistoryRelatedTypeMapper = enumMapper(LoyaltyPointsHistoryRelatedType.withName)
    implicit val loyaltyPointsHistoryTypeMapper = enumMapper(LoyaltyPointsHistoryType.withName)
    implicit val loyaltyProgramTypeMapper = enumMapper(LoyaltyProgramType.withName)
    implicit val merchantModeMapper = enumMapper(MerchantMode.withName)
    implicit val merchantSetupStepsMapper = enumMapper(MerchantSetupSteps.withName)
    implicit val modifierSetTypeMapper = enumMapper(ModifierSetType.withName)
    implicit val nextNumberTypeMapper = enumMapper(NextNumberType.withName)
    implicit val orderPaymentTypeMapper = enumMapper(OrderPaymentType.withName)
    implicit val orderStatusMapper = enumMapper(OrderStatus.withName)
    implicit val orderTypeMapper = enumMapper(OrderType.withName)
    implicit val paymentProcessorMapper = enumMapper(PaymentProcessor.withName)
    implicit val paymentStatusMapper = enumMapper(PaymentStatus.withName)
    implicit val paymentTransactionFeeTypeMapper = enumMapper(PaymentTransactionFeeType.withName)
    implicit val payScheduleMapper = enumMapper(PaySchedule.withName)
    implicit val purchaseOrderPaymentStatusMapper = enumMapper(PurchaseOrderPaymentStatus.withName)
    implicit val quantityChangeReasonMapper = enumMapper(QuantityChangeReason.withName)
    implicit val receivingObjectStatusMapper = enumMapper(ReceivingObjectStatus.withName)
    implicit val receivingOrderObjectTypeMapper = enumMapper(ReceivingOrderObjectType.withName)
    implicit val receivingOrderPaymentMethodMapper = enumMapper(ReceivingOrderPaymentMethod.withName)
    implicit val receivingOrderPaymentStatusMapper = enumMapper(ReceivingOrderPaymentStatus.withName)
    implicit val receivingOrderStatusMapper = enumMapper(ReceivingOrderStatus.withName)
    implicit val restaurantTypeMapper = enumMapper(RestaurantType.withName)
    implicit val returnReasonMapper = enumMapper(ReturnOrderReason.withName)
    implicit val returnStatusMapper = enumMapper(ReturnOrderStatus.withName)
    implicit val rewardRedemptionStatusMapper = enumMapper(RewardRedemptionStatus.withName)
    implicit val rewardRedemptionTypeMapper = enumMapper(RewardRedemptionType.withName)
    implicit val rewardTypeMapper = enumMapper(RewardType.withName)
    implicit val scopeTypeMapper = enumMapper(ScopeType.withName)
    implicit val setupTypeMapper = enumMapper(SetupType.withName)
    implicit val shiftStatusMapper = enumMapper(ShiftStatus.withName)
    implicit val soldByTypeMapper = enumMapper(UnitType.withName)
    implicit val sourceMapper = enumMapper(Source.withName)
    implicit val ticketStatusMapper = enumMapper(TicketStatus.withName)
    implicit val timeOffTypeMapper = enumMapper(TimeOffType.withName)
    implicit val tipsHandlingModeTypeMapper = enumMapper(TipsHandlingMode.withName)
    implicit val trackableActionMapper = enumMapper(TrackableAction.withName)
    implicit val transactionPaymentProcessorMapper = enumMapper(TransactionPaymentProcessor.withName)
    implicit val transactionPaymentTypeMapper = enumMapper(TransactionPaymentType.withName)
    implicit val transactionTypeMapper = enumMapper(TransactionType.withName)
    implicit val transferOrderTypeMapper = enumMapper(TransferOrderType.withName)
  }
}
