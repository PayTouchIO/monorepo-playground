package io.paytouch.core.utils

import java.time._
import java.util.{ Currency, UUID }

import scala.reflect.runtime.universe._

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator

import org.scalacheck._

import io.paytouch._

import io.paytouch.core.Availabilities
import io.paytouch.core.clients.urbanairship.entities.PassUpsertion
import io.paytouch.core.data.model
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.Weekdays.Day
import io.paytouch.utils.Generators

trait FixtureRandomGenerators extends CustomArbitraries

trait CustomArbitraries extends RandomDataGenerator with Generators {
  implicit val arbitraryWord: Arbitrary[String] = Arbitrary(genWord)
  implicit val arbitraryUUID: Arbitrary[UUID] = Arbitrary(Gen.uuid)
  implicit val arbitraryInt: Arbitrary[Int] = Arbitrary(genInt)
  implicit val arbitraryBoolean: Arbitrary[Boolean] = Arbitrary(genBoolean)
  implicit val arbitraryOptionalBoolean: Arbitrary[Option[Boolean]] = Arbitrary(genOptBoolean)
  implicit val arbitraryOptionalInt: Arbitrary[Option[Int]] = Arbitrary(genOptInt)

  implicit val arbitraryBigDecimal: Arbitrary[BigDecimal] = Arbitrary(genBigDecimal)
  implicit val arbitraryMonetaryAmount: Arbitrary[MonetaryAmount] = Arbitrary(genMonetaryAmount)
  implicit val arbitraryCurrency: Arbitrary[Currency] = Arbitrary(genCurrency)

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(genZonedDateTime)
  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary(genLocalTime)
  implicit val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(genZoneId)
  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(genLocalDate)

  implicit val arbitraryAddress: Arbitrary[Address] =
    Arbitrary(genAddress)

  implicit val arbitraryAddressImproved: Arbitrary[AddressImproved] =
    Arbitrary(genAddressImproved)

  implicit val arbitraryAddressImprovedSync: Arbitrary[AddressImprovedSync] =
    Arbitrary(genAddressImprovedSync)

  implicit val arbitraryAddressImprovedUpsertion: Arbitrary[AddressImprovedUpsertion] =
    Arbitrary(genAddressImprovedUpsertion)

  implicit val arbitraryAddressSync: Arbitrary[AddressSync] =
    Arbitrary(genAddressSync)

  implicit val arbitraryAddressUpsertion: Arbitrary[AddressUpsertion] =
    Arbitrary(genAddressUpsertion)

  implicit val arbitraryArticleCreation: Arbitrary[ArticleCreation] = Arbitrary(genArticleCreation)
  implicit val arbitraryArticleUpdate: Arbitrary[ArticleUpdate] = Arbitrary(genArticleUpdate)
  implicit val arbitraryVariantArticleCreation: Arbitrary[VariantArticleCreation] = Arbitrary(genVariantArticleCreation)
  implicit val arbitraryVariantArticleUpdate: Arbitrary[VariantArticleUpdate] = Arbitrary(genVariantArticleUpdate)

  implicit val arbitraryModifierOptionCount: Arbitrary[ModifierOptionCount] = Arbitrary(genModifierOptionCount)
  implicit val arbitraryProductCreation: Arbitrary[ProductCreation] = Arbitrary(genProductCreation)
  implicit val arbitraryProductUpdate: Arbitrary[ProductUpdate] = Arbitrary(genProductUpdate)
  implicit val arbitraryVariantProductCreation: Arbitrary[VariantProductCreation] = Arbitrary(genVariantProductCreation)
  implicit val arbitraryVariantProductUpdate: Arbitrary[VariantProductUpdate] = Arbitrary(genVariantProductUpdate)

  implicit val arbitraryPartCreation: Arbitrary[PartCreation] = Arbitrary(genPartCreation)
  implicit val arbitraryPartUpdate: Arbitrary[PartUpdate] = Arbitrary(genPartUpdate)
  implicit val arbitraryVariantPartCreation: Arbitrary[VariantPartCreation] = Arbitrary(genVariantPartCreation)
  implicit val arbitraryVariantPartUpdate: Arbitrary[VariantPartUpdate] = Arbitrary(genVariantPartUpdate)

  implicit val arbitraryRecipeCreation: Arbitrary[RecipeCreation] = Arbitrary(genRecipeCreation)
  implicit val arbitraryRecipeUpdate: Arbitrary[RecipeUpdate] = Arbitrary(genRecipeUpdate)

  implicit val arbitrarySendReceiptData: Arbitrary[SendReceiptData] = Arbitrary(genSendReceiptData)
  implicit val arbitraryPassUpsertion: Arbitrary[PassUpsertion] = Arbitrary(genPassUpsertion)

  implicit val arbOrderRoutingStatuses: Arbitrary[OrderRoutingStatusesByType] = Arbitrary(genOrderRoutingStatuses)
  implicit val arbitraryOrderItemVariantOptionUpsertion: Arbitrary[OrderItemVariantOptionUpsertion] = Arbitrary(
    genOrderItemVariantOptionUpsertion,
  )
  implicit val arbitraryPaymentDetails: Arbitrary[PaymentDetails] = Arbitrary(genPaymentDetails)
  implicit val arbUserCreation: Arbitrary[UserCreation] = Arbitrary(genUserCreation)
  implicit val arbUserUpdate: Arbitrary[UserUpdate] = Arbitrary(genUserUpdate)
  implicit val arbAdminCreation: Arbitrary[AdminCreation] = Arbitrary(genAdminCreation)
  implicit val arbAdminUpdate: Arbitrary[AdminUpdate] = Arbitrary(genAdminUpdate)

  // Enumeratum //

  implicit val arbitraryExposedName: Arbitrary[ExposedName] = Arbitrary(genExposedName)
  implicit val arbitraryModifierSetType: Arbitrary[ModifierSetType] = Arbitrary(genModifierSetType)
  implicit val arbitrarySource: Arbitrary[Source] = Arbitrary(genSource)
  implicit val arbitraryOrderType: Arbitrary[OrderType] = Arbitrary(genOrderType)
  implicit val arbitraryQuantityChangeReason: Arbitrary[QuantityChangeReason] = Arbitrary(genQuantityChangeReason)
  implicit val arbitraryChangeReason: Arbitrary[ChangeReason] = Arbitrary(genChangeReason)
  implicit val arbitraryOrderPaymentType: Arbitrary[OrderPaymentType] = Arbitrary(genOrderPaymentType)
  implicit val arbitraryTransactionPaymentType: Arbitrary[TransactionPaymentType] = Arbitrary(genTransactionPaymentType)
  implicit val arbitraryPaymentStatus: Arbitrary[PaymentStatus] = Arbitrary(genPaymentStatus)
  implicit val arbitraryOrderStatus: Arbitrary[OrderStatus] = Arbitrary(genOrderStatus)
  implicit val arbitraryFulfillmentStatus: Arbitrary[FulfillmentStatus] = Arbitrary(genFulfillmentStatus)
  implicit val arbitraryTransactionType: Arbitrary[TransactionType] = Arbitrary(genTransactionType)
  implicit val arbitraryAcceptanceStatus: Arbitrary[AcceptanceStatus] = Arbitrary(genAcceptanceStatus)
  implicit val arbitraryBusinessType: Arbitrary[BusinessType] = Arbitrary(genBusinessType)
  implicit val arbitraryRestaurantType: Arbitrary[RestaurantType] = Arbitrary(genRestaurantType)
  implicit val arbitraryDiscountType: Arbitrary[DiscountType] = Arbitrary(genDiscountType)
  implicit val arbitraryPaySchedule: Arbitrary[PaySchedule] = Arbitrary(genPaySchedule)
  implicit val arbitraryShiftStatus: Arbitrary[ShiftStatus] = Arbitrary(genShiftStatus)
  implicit val arbitraryTicketStatus: Arbitrary[TicketStatus] = Arbitrary(genTicketStatus)
  implicit val arbitraryTimeOff: Arbitrary[TimeOffType] = Arbitrary(genTimeOffType)
  implicit val arbitraryFrequencyInterval: Arbitrary[FrequencyInterval] = Arbitrary(genFrequencyInterval)
  implicit val arbitraryUnitType: Arbitrary[UnitType] = Arbitrary(genUnitType)
  implicit val arbitraryCashDrawerStatus: Arbitrary[CashDrawerStatus] = Arbitrary(genCashDrawerStatus)
  implicit val arbitraryCashDrawerActivityType: Arbitrary[CashDrawerActivityType] = Arbitrary(genCashDrawerActivityType)
  implicit val arbitraryLoyaltyProgramType: Arbitrary[LoyaltyProgramType] = Arbitrary(genLoyaltyProgramType)
  implicit val arbitraryRewardType: Arbitrary[RewardType] = Arbitrary(genRewardType)
  implicit val arbitraryPurchaseOrderPaymentStatus: Arbitrary[PurchaseOrderPaymentStatus] = Arbitrary(
    genPurchaseOrderPaymentStatus,
  )
  implicit val arbitraryReceivingObjectStatus: Arbitrary[ReceivingObjectStatus] = Arbitrary(genReceivingObjectStatus)
  implicit val arbitraryCardType: Arbitrary[CardType] = Arbitrary(genCardType)
  implicit val arbitraryCardTransactionResultType: Arbitrary[CardTransactionResultType] = Arbitrary(
    genCardTransactionResultType,
  )
  implicit val arbitraryCardTransactionStatusType: Arbitrary[CardTransactionStatusType] = Arbitrary(
    genCardTransactionStatusType,
  )
  implicit val arbitraryMerchantSetupSteps: Arbitrary[MerchantSetupSteps] = Arbitrary(genMerchantSetupSteps)

  // Empty options //

  implicit val arbitraryOptionUuid: Arbitrary[Option[UUID]] = Arbitrary(None)
  implicit val arbitraryOptionLocalDate: Arbitrary[Option[LocalDate]] = Arbitrary(None)
  implicit val arbitraryMerchant: Arbitrary[Option[Merchant]] = Arbitrary(None)
  implicit val arbitrarySubcategory: Arbitrary[Option[Seq[io.paytouch.core.entities.Category]]] = Arbitrary(None)
  implicit val arbitraryOptionPurchaseOrderProducts: Arbitrary[Option[Seq[PurchaseOrderProductUpsertion]]] = Arbitrary(
    None,
  )
  implicit val arbitraryOptionPurchaseOrder: Arbitrary[Option[PurchaseOrder]] = Arbitrary(None)
  implicit val arbitraryOptItemLocationOverrides: Arbitrary[Option[Map[UUID, ItemLocation]]] = Arbitrary(None)
  implicit val arbitraryOptionAuth0UserId: Arbitrary[Option[Auth0UserId]] = Arbitrary(None)

  // Empty sequences //

  implicit val arbitraryAny: Arbitrary[Seq[Any]] = Arbitrary(Seq.empty[Any])
  implicit val arbitraryLocations: Arbitrary[Seq[Location]] = Arbitrary(Seq.empty[Location])
  implicit val arbitraryProducts: Arbitrary[Seq[Product]] = Arbitrary(Seq.empty[Product])
  implicit val arbitraryBundleSets: Arbitrary[Seq[BundleSet]] = Arbitrary(Seq.empty[BundleSet])
  implicit val arbitraryLocationPrices: Arbitrary[Seq[ProductLocation]] = Arbitrary(Seq.empty[ProductLocation])
  implicit val arbitraryCategories: Arbitrary[Seq[Category]] = Arbitrary(Seq.empty[Category])
  implicit val arbitraryCategoryPosition: Arbitrary[Seq[CategoryPosition]] = Arbitrary(Seq.empty[CategoryPosition])
  implicit val arbitraryCatalogCategoryOption: Arbitrary[Seq[CatalogCategoryOption]] = Arbitrary(
    Seq.empty[CatalogCategoryOption],
  )
  implicit val arbitraryMerchantNotes: Arbitrary[Seq[MerchantNote]] = Arbitrary(Seq.empty[MerchantNote])
  implicit val arbitraryMerchantNoteUpsertions: Arbitrary[Seq[MerchantNoteUpsertion]] = Arbitrary(
    Seq.empty[MerchantNoteUpsertion],
  )
  implicit val arbitraryModifierSet: Arbitrary[Seq[ModifierSet]] = Arbitrary(Seq.empty[ModifierSet])
  implicit val arbitraryModifierOption: Arbitrary[Seq[ModifierOption]] = Arbitrary(Seq.empty[ModifierOption])
  implicit val arbitraryModifierPosition: Arbitrary[Seq[ModifierPosition]] = Arbitrary(Seq.empty[ModifierPosition])
  implicit val arbitraryOrderItemUpsertions: Arbitrary[Seq[OrderItemUpsertion]] = Arbitrary(
    Seq.empty[OrderItemUpsertion],
  )
  implicit val arbitraryOrderItemDiscountUpsertions: Arbitrary[Seq[ItemDiscountUpsertion]] = Arbitrary(
    Seq.empty[ItemDiscountUpsertion],
  )
  implicit val arbitraryOrderItemModifierOptionUpsertions: Arbitrary[Seq[OrderItemModifierOptionUpsertion]] =
    Arbitrary(Seq.empty[OrderItemModifierOptionUpsertion])
  implicit val arbitraryOrderItemTaxRateUpsertions: Arbitrary[Seq[OrderItemTaxRateUpsertion]] =
    Arbitrary(Seq.empty[OrderItemTaxRateUpsertion])
  implicit val arbitraryOrderTaxRates: Arbitrary[Seq[OrderTaxRate]] =
    Arbitrary(Seq.empty[OrderTaxRate])
  implicit val arbitraryOrderTaxRateUpsertions: Arbitrary[Seq[OrderTaxRateUpsertion]] =
    Arbitrary(Seq.empty[OrderTaxRateUpsertion])
  implicit val arbitraryOrderItemVariantOptionUpsertions: Arbitrary[Seq[OrderItemVariantOptionUpsertion]] = Arbitrary(
    Seq.empty[OrderItemVariantOptionUpsertion],
  )
  implicit val arbitraryPaymentTransactions: Arbitrary[Seq[PaymentTransaction]] = Arbitrary(
    Seq.empty[PaymentTransaction],
  )
  implicit val arbitraryPaymentTransactionsUpsertion: Arbitrary[Seq[PaymentTransactionUpsertion]] = Arbitrary(
    Seq.empty[PaymentTransactionUpsertion],
  )
  implicit val arbitrarySeqUUID: Arbitrary[Seq[UUID]] = Arbitrary(Seq.empty[UUID])
  implicit val arbitraryStatusTransitions: Arbitrary[Seq[model.StatusTransition]] = Arbitrary(
    Seq.empty[model.StatusTransition],
  )
  implicit val arbitrarySubcategoryUpsertion: Arbitrary[Seq[SubcategoryUpsertion]] = Arbitrary(
    Seq.empty[SubcategoryUpsertion],
  )
  implicit val arbitraryVariantOptionTypes: Arbitrary[Seq[VariantOptionType]] = Arbitrary(Seq.empty[VariantOptionType])
  implicit val arbitraryVariantOptionWithTypes: Arbitrary[Seq[VariantOptionWithType]] = Arbitrary(
    Seq.empty[VariantOptionWithType],
  )
  implicit val arbitraryVariantProductCreations: Arbitrary[Seq[VariantProductCreation]] = Arbitrary(
    Seq.empty[VariantProductCreation],
  )
  implicit val arbitraryVariantProductUpdates: Arbitrary[Seq[VariantProductUpdate]] = Arbitrary(
    Seq.empty[VariantProductUpdate],
  )
  implicit val arbitraryUserInfos: Arbitrary[Seq[UserInfo]] = Arbitrary(Seq.empty[UserInfo])
  implicit val arbitraryOrderBundles: Arbitrary[Seq[OrderBundle]] = Arbitrary(Seq.empty[OrderBundle])
  implicit val arbitraryOrderDiscounts: Arbitrary[Seq[OrderDiscount]] = Arbitrary(Seq.empty[OrderDiscount])
  implicit val arbitraryOrderBundleSets: Arbitrary[Seq[OrderBundleSet]] = Arbitrary(Seq.empty[OrderBundleSet])
  implicit val arbitraryOrderBundleOptions: Arbitrary[Seq[OrderBundleOption]] = Arbitrary(Seq.empty[OrderBundleOption])
  implicit val arbitraryOrderItems: Arbitrary[Seq[OrderItem]] = Arbitrary(Seq.empty[OrderItem])
  implicit val arbitrarySupplierInfos: Arbitrary[Seq[SupplierInfo]] = Arbitrary(Seq.empty[SupplierInfo])
  implicit val arbitraryTaxRates: Arbitrary[Seq[TaxRate]] = Arbitrary(Seq.empty[TaxRate])
  implicit val arbitraryTicketInfos: Arbitrary[Seq[TicketInfo]] = Arbitrary(Seq.empty[TicketInfo])
  implicit val arbitraryRecipeDetail: Arbitrary[Seq[RecipeDetail]] = Arbitrary(Seq.empty[RecipeDetail])
  implicit val arbitraryImageUrls: Arbitrary[Seq[ImageUrls]] = Arbitrary(Seq.empty[ImageUrls])
  implicit val arbitraryMonetaryAmounts: Arbitrary[Seq[MonetaryAmount]] = Arbitrary(Seq.empty[MonetaryAmount])
  implicit val arbitraryCustomerNotes: Arbitrary[Seq[model.CustomerNote]] = Arbitrary(Seq.empty[model.CustomerNote])
  implicit val arbitraryLoyaltyRewards: Arbitrary[Seq[LoyaltyReward]] = Arbitrary(Seq.empty[LoyaltyReward])
  implicit val arbitraryLoyaltyMemberships: Arbitrary[Seq[LoyaltyMembership]] = Arbitrary(Seq.empty[LoyaltyMembership])
  implicit val arbitraryRewardRedemptions: Arbitrary[Seq[RewardRedemption]] = Arbitrary(Seq.empty[RewardRedemption])
  implicit val arbitraryRewardRedemptionSyncs: Arbitrary[Seq[RewardRedemptionSync]] = Arbitrary(
    Seq.empty[RewardRedemptionSync],
  )
  implicit val arbitraryGiftCardPassTransactions: Arbitrary[Seq[GiftCardPassTransaction]] = Arbitrary(
    Seq.empty[GiftCardPassTransaction],
  )
  implicit val arbitraryPaymentTransactionFees: Arbitrary[Seq[PaymentTransactionFee]] = Arbitrary(
    Seq.empty[PaymentTransactionFee],
  )

  // Empty maps //

  implicit val arbitraryOpeningHours: Arbitrary[Availabilities] = Arbitrary(Map.empty[Day, Seq[Availability]])
  implicit val arbitraryLocationOverrides: Arbitrary[Map[UUID, Option[CategoryLocationUpdate]]] = Arbitrary(
    Map.empty[UUID, Option[CategoryLocationUpdate]],
  )
  implicit val arbitraryItemLocationUpdateOverrides: Arbitrary[Map[UUID, Option[ItemLocationUpdate]]] = Arbitrary(
    Map.empty[UUID, Option[ItemLocationUpdate]],
  )
  implicit val arbitraryItemLocationOverrides: Arbitrary[Map[UUID, ItemLocation]] = Arbitrary(
    Map.empty[UUID, ItemLocation],
  )
  implicit val arbitraryCategoryLocationOverrides: Arbitrary[Map[UUID, CategoryLocation]] = Arbitrary(
    Map.empty[UUID, CategoryLocation],
  )
  implicit val arbitraryProductLocationOverrides: Arbitrary[Map[UUID, ProductLocation]] = Arbitrary(
    Map.empty[UUID, ProductLocation],
  )
  implicit val arbitraryTaxRateLocationOverrides: Arbitrary[Map[UUID, Option[TaxRateLocationUpdate]]] = Arbitrary(
    Map.empty[UUID, Option[TaxRateLocationUpdate]],
  )
  implicit val arbitraryProductLocationUpdate: Arbitrary[Map[UUID, Option[ArticleLocationUpdate]]] = Arbitrary(
    Map.empty[UUID, Option[ArticleLocationUpdate]],
  )

  implicit val arbitraryOnlineCode: Arbitrary[io.paytouch.GiftCardPass.OnlineCode] = Arbitrary(genOnlineCode)
  implicit val arbitraryStringAny: Arbitrary[Map[String, Any]] = Arbitrary(Map.empty[String, Any])
  implicit val arbitraryStringString: Arbitrary[Map[String, String]] = Arbitrary(Map.empty[String, String])

  // Resettable //

  implicit val arbitraryResettableString: Arbitrary[ResettableString] = Arbitrary(genResettableString)
  implicit val arbitraryResettableBigDecimal: Arbitrary[ResettableBigDecimal] = Arbitrary(genResettableBigDecimal)
  implicit val arbitraryResettableUUID: Arbitrary[ResettableUUID] = Arbitrary(genResettableUUID)
  implicit val arbitraryResettableLocalDate: Arbitrary[ResettableLocalDate] = Arbitrary(genResettableLocalDate)
  implicit val arbitraryResettableZonedDateTime: Arbitrary[ResettableZonedDateTime] = Arbitrary(
    genResettableZonedDateTime,
  )

  /**
    * Same as random but clarifies the fact that the values are generated only once per test run.
    */
  final def randomOnce[T: WeakTypeTag: Arbitrary]: T = random

  /**
    * Same as random but clarifies the fact that the values are generated only once per test run.
    */
  final def randomOnce[T: WeakTypeTag: Arbitrary](n: Int): Seq[T] = random(n)
}
