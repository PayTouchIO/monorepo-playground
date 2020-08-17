package io.paytouch.core.services

import scala.concurrent._

import akka.actor.{ ActorRef, ActorSystem }

import cats.implicits._

import awscala.s3.Bucket
import com.softwaremill.macwire._
import io.paytouch.core.async.monitors.Monitors
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.clients.auth0._
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.clients.stripe._
import io.paytouch.core.clients.urbanairship.WalletClient
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.entities.PassLoaderFactory
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.processors._
import io.paytouch.core.reports.services.{ AdminReportService, EngineService, ExportService, QueryService }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.{
  CloudfrontImagesDistribution,
  CloudinaryUrl,
  S3CashDrawerActivitiesBucket,
  S3ExportsBucket,
  S3ImagesBucket,
  S3ImportsBucket,
  ServiceConfigurations => Config,
}
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

import scala.concurrent.ExecutionContext
import io.paytouch.core.messages.entities.StoreCreated

trait Services { self: Monitors =>
  implicit def ec: ExecutionContext
  implicit def db: Database

  def slowOpsDb: Database

  implicit protected def system: ActorSystem
  implicit def mdcActor: ActorRef withTag BaseMdcActor
  protected def s3Client: S3Client
  protected def ptOrderingClient: PtOrderingClient
  protected def stripeClient: StripeClient
  protected def stripeConnectClient: StripeConnectClient
  protected def jwkClient: JwkClient
  protected def auth0Client: Auth0Client

  private lazy val jwtSecret = Config.JwtSecret
  private lazy val bcryptRounds = Config.bcryptRounds
  private lazy val hmacSecret = Config.hmacSecret

  private lazy val uploadFolder = Config.uploadFolder

  private lazy val urbanAirshipHost = Config.urbanAirshipHost
  private lazy val urbanAirshipUsername = Config.urbanAirshipUsername
  private lazy val urbanAirshipApiKey = Config.urbanAirshipApiKey
  private lazy val urbanAirshipProjectIds = Config.urbanAirshipProjectIds

  private lazy val ptCoreUrl = Config.ptCoreURL

  private lazy val pusherKey = Config.pusherKey
  private lazy val pusherSecret = Config.pusherSecret

  private lazy val cloudfrontImagesDistribution: String withTag CloudfrontImagesDistribution =
    Config.cloudfrontImagesDistribution

  private lazy val cloudinaryUrl: String withTag CloudinaryUrl =
    Config.cloudinaryUrl

  protected lazy val imagesBucket: Bucket withTag S3ImagesBucket =
    s3Client.createOrGetBucket(Config.s3ImagesBucket).taggedWith[S3ImagesBucket]

  protected lazy val exportsBucket: Bucket withTag S3ExportsBucket =
    s3Client.createOrGetBucket(Config.s3ExportsBucket).taggedWith[S3ExportsBucket]

  protected lazy val importsBucket: Bucket withTag S3ImportsBucket =
    s3Client.createOrGetBucket(Config.s3ImportsBucket).taggedWith[S3ImportsBucket]

  protected lazy val cashDrawerActivitiesBucket: Bucket withTag S3CashDrawerActivitiesBucket =
    s3Client.createOrGetBucket(Config.s3CashDrawerActivitiesBucket).taggedWith[S3CashDrawerActivitiesBucket]

  protected lazy val adminPasswordAuthEnabled = Config.adminPasswordAuthEnabled

  private lazy val googleAuthClientId = Config.googleAuthClientId

  def generateOnlineCode: GiftCardPassService.GenerateOnlineCode =
    GiftCardPassService.generateOnlineCode

  implicit lazy val daos = new Daos
  implicit lazy val paytouchLogger = new PaytouchLogger

  lazy val adminAuthenticationService = wire[AdminAuthenticationService]
  lazy val adminMerchantService = wire[AdminMerchantService]
  lazy val adminReportService = new AdminReportService(slowOpsDb)
  lazy val adminService = wire[AdminService]
  lazy val articleService = wire[ArticleService]
  lazy val auth0Service = wire[Auth0Service]
  lazy val authenticationService = wire[AuthenticationService]
  lazy val barcodeService = wire[BarcodeService]
  lazy val brandService = wire[BrandService]
  lazy val bundleOptionService: BundleOptionService = wire[BundleOptionService]
  lazy val bundleSetService = wire[BundleSetService]
  lazy val cashDrawerActivityService = wire[CashDrawerActivityService]
  lazy val cashDrawerService = wire[CashDrawerService]
  lazy val catalogAvailabilityService = wire[CatalogAvailabilityService]
  lazy val catalogCategoryService = wire[CatalogCategoryService]
  lazy val catalogService = wire[CatalogService]
  lazy val categoryAvailabilityService: CategoryAvailabilityService = wire[CategoryAvailabilityService]
  lazy val categoryLocationAvailabilityService = wire[CategoryLocationAvailabilityService]
  lazy val categoryLocationService: CategoryLocationService = wire[CategoryLocationService]
  lazy val categoryService = wire[CategoryService]
  lazy val commentService = wire[CommentService]
  lazy val customerGroupService = wire[CustomerGroupService]
  lazy val customerLocationService = wire[CustomerLocationService]
  lazy val customerMerchantService: CustomerMerchantService = wire[CustomerMerchantService]
  lazy val customerMerchantSyncService = wire[CustomerMerchantSyncService]
  lazy val discountAvailabilityService = wire[DiscountAvailabilityService]
  lazy val discountLocationService = wire[DiscountLocationService]
  lazy val discountService = wire[DiscountService]
  lazy val engineService = wire[EngineService]
  lazy val eventService = wire[EventService]
  lazy val exportService = wire[ExportService]
  lazy val featureGroupService = wire[FeatureGroupService]
  lazy val giftCardPassService: GiftCardPassService = wire[GiftCardPassService]
  lazy val giftCardPassTransactionService = wire[GiftCardPassTransactionService]
  lazy val giftCardService: GiftCardService = wire[GiftCardService]
  lazy val globalCustomerService = wire[GlobalCustomerService]
  lazy val googleAuthenticationService = wire[GoogleAuthenticationService]
  lazy val groupService = wire[GroupService]
  lazy val hmacService = wire[HmacService]
  lazy val imageUploadService = wire[ImageUploadService]
  lazy val importService = wire[ImportService]
  lazy val inventoryCountProductService = wire[InventoryCountProductService]
  lazy val inventoryCountService = wire[InventoryCountService]
  lazy val inventoryService = wire[InventoryService]
  lazy val kitchenService = wire[KitchenService]
  lazy val locationAvailabilityService = wire[LocationAvailabilityService]
  lazy val locationEmailReceiptService = wire[LocationEmailReceiptService]
  lazy val locationPrintReceiptService = wire[LocationPrintReceiptService]
  lazy val locationReceiptService = wire[LocationReceiptService]
  lazy val locationService: LocationService = wire[LocationService]
  lazy val locationSettingsService = wire[LocationSettingsService]
  lazy val loyaltyMembershipService: LoyaltyMembershipService = wire[LoyaltyMembershipService]
  lazy val loyaltyPointsHistoryService = wire[LoyaltyPointsHistoryService]
  lazy val loyaltyProgramLocationService = wire[LoyaltyProgramLocationService]
  lazy val loyaltyProgramService = wire[LoyaltyProgramService]
  lazy val loyaltyRewardProductService = wire[LoyaltyRewardProductService]
  lazy val loyaltyRewardService: LoyaltyRewardService = wire[LoyaltyRewardService]
  lazy val merchantService: MerchantService = wire[MerchantService]
  lazy val modifierOptionService = wire[ModifierOptionService]
  lazy val modifierSetLocationService = wire[ModifierSetLocationService]
  lazy val modifierSetProductService = wire[ModifierSetProductService]
  lazy val modifierSetService = wire[ModifierSetService]
  lazy val oauthService: OauthService = wire[OauthService]
  lazy val onlineOrderAttributeService: OnlineOrderAttributeService = wire[OnlineOrderAttributeService]
  lazy val orderBundleService = wire[OrderBundleService]
  lazy val orderDeliveryAddressService = wire[OrderDeliveryAddressService]
  lazy val orderDiscountService = wire[OrderDiscountService]
  lazy val orderFeedbackService = wire[OrderFeedbackService]
  lazy val orderItemDiscountService = wire[OrderItemDiscountService]
  lazy val orderItemModifierOptionService = wire[OrderItemModifierOptionService]
  lazy val orderItemService = wire[OrderItemService]
  lazy val orderItemTaxRateService = wire[OrderItemTaxRateService]
  lazy val orderItemVariantOptionService = wire[OrderItemVariantOptionService]
  lazy val orderService: OrderService = wire[OrderService]
  lazy val orderSyncService: OrderSyncService = wire[OrderSyncService]
  lazy val orderTaxRateService = wire[OrderTaxRateService]
  lazy val orderUserService = wire[OrderUserService]
  lazy val partService = wire[PartService]
  lazy val passLoaderFactory = wire[PassLoaderFactory]
  lazy val passService = wire[PassService]
  lazy val passwordResetService = wire[PasswordResetService]
  lazy val paymentTransactionFeeService = wire[PaymentTransactionFeeService]
  lazy val paymentTransactionOrderItemService = wire[PaymentTransactionOrderItemService]
  lazy val paymentTransactionService = wire[PaymentTransactionService]
  lazy val payrollService = wire[PayrollService]
  lazy val productCategoryOptionService = wire[ProductCategoryOptionService]
  lazy val productCategoryService = wire[ProductCategoryService]
  lazy val productCostHistoryService = wire[ProductCostHistoryService]
  lazy val productImportService = wire[ProductImportService]
  lazy val productLocationService = wire[ProductLocationService]
  lazy val productPartService = wire[ProductPartService]
  lazy val productPriceHistoryService = wire[ProductPriceHistoryService]
  lazy val productQuantityHistoryService = wire[ProductQuantityHistoryService]
  lazy val productService = wire[ProductService]
  lazy val purchaseOrderProductService: PurchaseOrderProductService = wire[PurchaseOrderProductService]
  lazy val purchaseOrderService: PurchaseOrderService = wire[PurchaseOrderService]
  lazy val pusherService = wire[PusherService]
  lazy val queryService = wire[QueryService]
  lazy val receivingOrderProductService: ReceivingOrderProductService = wire[ReceivingOrderProductService]
  lazy val receivingOrderService: ReceivingOrderService = wire[ReceivingOrderService]
  lazy val recipeDetailService = wire[RecipeDetailService]
  lazy val reportService = wire[ReportService]
  lazy val returnOrderProductService: ReturnOrderProductService = wire[ReturnOrderProductService]
  lazy val returnOrderService: ReturnOrderService = wire[ReturnOrderService]
  lazy val rewardRedemptionService: RewardRedemptionService = wire[RewardRedemptionService]
  lazy val sampleDataService: SampleDataService = wire[SampleDataService]
  lazy val setupStepService = wire[SetupStepService]
  lazy val shiftService = wire[ShiftService]
  lazy val stockModifierService = wire[StockModifierService]
  lazy val stockService = wire[StockService]
  lazy val storeService = wire[StoreService]
  lazy val stripeService = wire[StripeService]
  lazy val supplierLocationService = wire[SupplierLocationService]
  lazy val supplierProductService = wire[SupplierProductService]
  lazy val supplierService = wire[SupplierService]
  lazy val systemCategoryService = wire[SystemCategoryService]
  lazy val taxRateLocationService = wire[TaxRateLocationService]
  lazy val taxRateService = wire[TaxRateService]
  lazy val ticketOrderItemService: TicketOrderItemService = wire[TicketOrderItemService]
  lazy val ticketService = wire[TicketService]
  lazy val timeCardService = wire[TimeCardService]
  lazy val timeOffCardService = wire[TimeOffCardService]
  lazy val tipsAssignmentService = wire[TipsAssignmentService]
  lazy val transferOrderProductService = wire[TransferOrderProductService]
  lazy val transferOrderService: TransferOrderService = wire[TransferOrderService]
  lazy val urbanAirshipService = wire[UrbanAirshipService]
  lazy val userLocationService = wire[UserLocationService]
  lazy val userRoleService: UserRoleService = wire[UserRoleService]
  lazy val userService = wire[UserService]
  lazy val validatorService = wire[ValidatorService]
  lazy val variantProductService = wire[VariantArticleService]
  lazy val variantService = wire[VariantService]

  lazy val walletClient =
    new WalletClient(urbanAirshipHost, urbanAirshipUsername, urbanAirshipApiKey)(mdcActor, system)

  lazy val messageHandler = wire[SQSMessageHandler]
  lazy val giftCardChangedProcessor = wire[GiftCardChangedProcessor]
  lazy val giftCardPassChangedProcessor = wire[GiftCardPassChangedProcessor]
  lazy val imagesProcessor = wire[ImagesProcessor]
  lazy val loyaltyProgramChangedProcessor = wire[LoyaltyProgramChangedProcessor]
  lazy val loyaltyMembershipChangedProcessor = wire[LoyaltyMembershipChangedProcessor]
  lazy val orderSyncedProcessor = wire[OrderSyncedProcessor]
  lazy val prepareCashDrawerActivityProcessor = wire[PrepareCashDrawerActivityProcessor]
  lazy val prepareOrderReceiptProcessor = wire[PrepareOrderReceiptProcessor]
  lazy val prepareLoyaltyProgramSignedUpProcessor = wire[PrepareLoyaltyProgramSignedUpProcessor]
  lazy val prepareGiftCardPassReceiptRequestedProcessor = wire[PrepareGiftCardPassReceiptRequestedProcessor]
  lazy val storeCreatedProcessor = wire[StoreCreatedProcessor]
  lazy val storesActiveChangedProcessor = wire[StoresActiveChangedProcessor]
  lazy val rapidoChangedProcessor = wire[RapidoChangedProcessor]
}
