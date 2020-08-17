package io.paytouch.core.services

import akka.testkit.TestProbe

import org.specs2.specification.Scope

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.data.daos.{ ConfiguredTestDatabase, Daos }
import io.paytouch.core.utils._
import io.paytouch.utils.Tagging._

abstract class ServiceDaoSpec extends PaytouchSpec with LiquibaseSupportProvider with ConfiguredTestDatabase {
  implicit lazy val daos: Daos = new Daos

  abstract class ServiceDaoSpecContext
      extends ServiceDaoSpecBaseContext
         with MultipleLocationFixtures
         with ValidatedHelpers {
    implicit val userCtx = userContext

    def generateOnlineCode: GiftCardPassService.GenerateOnlineCode =
      GiftCardPassService.generateOnlineCode
  }

  abstract class ServiceDaoSpecBaseContext extends Scope with ValidatedHelpers {
    val actorSystem = MockedRestApi.testAsyncSystem
    val actorMock = new TestProbe(actorSystem)

    val eventTrackerMock = new TestProbe(actorSystem)
    val eventTracker = eventTrackerMock.ref.taggedWith[EventTracker]

    val articleService = MockedRestApi.articleService
    val auth0Service = MockedRestApi.auth0Service
    val barcodeService = MockedRestApi.barcodeService
    val bundleSetService = MockedRestApi.bundleSetService
    val categoryService = MockedRestApi.categoryService
    val cashDrawerService = MockedRestApi.cashDrawerService
    val cashDrawerActivityService = MockedRestApi.cashDrawerActivityService
    val catalogService = MockedRestApi.catalogService
    val catalogCategoryService = MockedRestApi.catalogCategoryService
    val commentService = MockedRestApi.commentService
    val customerLocationService = MockedRestApi.customerLocationService
    val locationSettingsService = MockedRestApi.locationSettingsService
    val loyaltyMembershipService = MockedRestApi.loyaltyMembershipService
    val customerMerchantService = MockedRestApi.customerMerchantService
    val customerMerchantSyncService = MockedRestApi.customerMerchantSyncService
    val giftCardService = MockedRestApi.giftCardService
    val giftCardPassService = MockedRestApi.giftCardPassService
    val giftCardPassTransactionService = MockedRestApi.giftCardPassTransactionService
    val globalCustomerService = MockedRestApi.globalCustomerService
    val googleAuthenticationService = MockedRestApi.googleAuthenticationService
    val hmacService = MockedRestApi.hmacService
    val imageUploadService = MockedRestApi.imageUploadService
    val kitchenService = MockedRestApi.kitchenService
    val locationEmailReceiptService = MockedRestApi.locationEmailReceiptService
    val locationPrintReceiptService = MockedRestApi.locationPrintReceiptService
    val locationReceiptService = MockedRestApi.locationReceiptService
    val locationService = MockedRestApi.locationService
    val loyaltyPointsHistoryService = MockedRestApi.loyaltyPointsHistoryService
    val loyaltyProgramService = MockedRestApi.loyaltyProgramService
    val loyaltyProgramLocationService = MockedRestApi.loyaltyProgramLocationService
    val loyaltyRewardService = MockedRestApi.loyaltyRewardService
    val loyaltyRewardProductService = MockedRestApi.loyaltyRewardProductService
    val merchantService = MockedRestApi.merchantService
    val modifierSetProductService = MockedRestApi.modifierSetProductService
    val modifierSetService = MockedRestApi.modifierSetService
    val orderDeliveryAddressService = MockedRestApi.orderDeliveryAddressService
    val onlineOrderAttributeService = MockedRestApi.onlineOrderAttributeService
    val orderBundleService = MockedRestApi.orderBundleService
    val orderDiscountService = MockedRestApi.orderDiscountService
    val orderItemService = MockedRestApi.orderItemService
    val orderService: OrderService = MockedRestApi.orderService
    val orderSyncService = MockedRestApi.orderSyncService
    val orderTaxRateService = MockedRestApi.orderTaxRateService
    val orderItemTaxRateService = MockedRestApi.orderItemTaxRateService
    val orderUserService = MockedRestApi.orderUserService
    val passService = MockedRestApi.passService
    val paymentTransactionService = MockedRestApi.paymentTransactionService
    val paymentTransactionFeeService = MockedRestApi.paymentTransactionFeeService
    val paymentTransactionOrderItemService = MockedRestApi.paymentTransactionOrderItemService
    val productCategoryService = MockedRestApi.productCategoryService
    val productCategoryOptionService = MockedRestApi.productCategoryOptionService
    val productLocationService = MockedRestApi.productLocationService
    val productPartService = MockedRestApi.productPartService
    val purchaseOrderService = MockedRestApi.purchaseOrderService
    val purchaseOrderProductService = MockedRestApi.purchaseOrderProductService
    val receivingOrderService = MockedRestApi.receivingOrderService
    val receivingOrderProductService = MockedRestApi.receivingOrderProductService
    val recipeDetailService = MockedRestApi.recipeDetailService
    val rewardRedemptionService = MockedRestApi.rewardRedemptionService
    val sampleDataService = MockedRestApi.sampleDataService
    val setupStepService = MockedRestApi.setupStepService
    val systemCategoryService = MockedRestApi.systemCategoryService
    val stockModifierService = MockedRestApi.stockModifierService
    val stockService: StockService = MockedRestApi.stockService
    val storeService = MockedRestApi.storeService
    val supplierProductService = MockedRestApi.supplierProductService
    val supplierService = MockedRestApi.supplierService
    val taxRateLocationService = MockedRestApi.taxRateLocationService
    val taxRateService = MockedRestApi.taxRateService
    val ticketOrderItemService = MockedRestApi.ticketOrderItemService
    val ticketService = MockedRestApi.ticketService
    val tipsAssignmentService = MockedRestApi.tipsAssignmentService
    val transferOrderService = MockedRestApi.transferOrderService
    val urbanAirshipService = MockedRestApi.urbanAirshipService
    val userLocationService = MockedRestApi.userLocationService
    val userRoleService = MockedRestApi.userRoleService
    val userService = MockedRestApi.userService
    val variantProductService = MockedRestApi.variantProductService
    val variantService = MockedRestApi.variantService
  }
}
