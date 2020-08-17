package io.paytouch.core

import akka.http.scaladsl.server._
import io.paytouch.core.async.monitors.Monitors
import io.paytouch.core.async.sqs.SQSConsumers
import io.paytouch.core.barcodes.resources.BarcodeResource
import io.paytouch.core.logging.HttpLogging
import io.paytouch.core.reports.resources.{ AdminReportResource, EngineResource, ExportResource }
import io.paytouch.core.resources._
import io.paytouch.core.services._
import io.paytouch.core.utils.CustomHandlers

trait RestApi
    extends JsonResources
       with FormDataResources
       with Services
       with Monitors
       with SQSConsumers
       with HttpLogging
       with CustomHandlers {

  lazy val routes: Route =
    customLogRequestResponse {
      cors {
        concat(
          pingRoutes,
          pathPrefix("v1") {
            concat(
              allAdminRoutes,
              apiRoutes,
              publicRoutes,
            )
          },
          resourceVersionedRoutes,
          rejectNonMatchedRoutes,
        )
      }
    }

  private lazy val allAdminRoutes =
    pathPrefix("admin") {
      concat(
        adminReportRoutes,
        adminRoutes,
        merchantAdminRoutes,
      )
    }

  private lazy val publicRoutes =
    concat(
      merchantPublicRoutes,
      passRoutes,
    )

  private lazy val apiRoutes =
    concat(
      auth0Routes,
      barcodeRoutes,
      brandRoutes,
      cashDrawerActivityRoutes,
      cashDrawerRoutes,
      catalogCategoryRoutes,
      catalogRoutes,
      customerMerchantRoutes,
      discountRoutes,
      engineRoutes,
      eventRoutes,
      exportRoutes,
      giftCardPassRoutes,
      giftCardRoutes,
      groupRoutes,
      imageUploadFormRoutes,
      importRoutes,
      inventoryCountRoutes,
      kitchenRoutes,
      locationRoutes,
      loyaltyMembershipRoutes,
      loyaltyProgramRoutes,
      loyaltyRewardRoutes,
      merchantApiRoutes,
      modifierSetRoutes,
      oauthRoutes,
      orderFeedbackRoutes,
      orderRoutes,
      passRoutes,
      paymentTransactionRoutes,
      payrollRoutes,
      productImportRoutes,
      productRoutes,
      purchaseOrderRoutes,
      pusherRoutes,
      receivingOrderRoutes,
      reportRoutes,
      returnOrderRoutes,
      sessionRoutes,
      shiftRoutes,
      stockRoutes,
      stripeRoutes,
      supplierRoutes,
      swaggerRoutes,
      systemCategoryRoutes,
      taxRateRoutes,
      ticketRoutes,
      timeCardRoutes,
      timeOffCardsRoutes,
      tipsAssignmentsRoutes,
      transferOrderRoutes,
      userRoleRoutes,
      userRoutes,
      utilRoutes,
      validatorRoutes,
    )

  private lazy val resourceVersionedRoutes =
    concat(
      imageUploadRoutes,
    )
}

trait JsonResources
    extends AdminReportResource
       with AdminResource
       with Auth0Resource
       with ArticleResource
       with BarcodeResource
       with BrandResource
       with CashDrawerActivityResource
       with CashDrawerResource
       with CatalogCategoryResource
       with CatalogResource
       with CommentResource
       with CustomerMerchantResource
       with DiscountResource
       with EngineResource
       with EventResource
       with ExportResource
       with GiftCardPassResource
       with GiftCardResource
       with GroupResource
       with ImageUploadResource
       with ImportResource
       with InventoryCountResource
       with InventoryResource
       with KitchenResource
       with LocationResource
       with LoyaltyMembershipResource
       with LoyaltyProgramResource
       with LoyaltyRewardResource
       with MerchantResource
       with ModifierSetResource
       with OauthResource
       with OrderFeedbackResource
       with OrderResource
       with PartResource
       with PassResource
       with PaymentTransactionResource
       with PayrollResource
       with PingResource
       with ProductCategoryResource
       with ProductHistoryResource
       with ProductPartResource
       with ProductResource
       with PurchaseOrderResource
       with ReceivingOrderResource
       with ReportResource
       with ReturnOrderResource
       with SessionResource
       with ShiftResource
       with StockResource
       with StripeResource
       with SupplierResource
       with SwaggerResource
       with SystemCategoryResource
       with TaxRateResource
       with TicketResource
       with TimeCardResource
       with TimeOffCardResource
       with TipsAssignmentResource
       with TransferOrderResource
       with UserResource
       with UserRoleResource
       with UtilResource
       with ValidatorResource

trait FormDataResources extends ImageUploadFormResource with ProductImportFormResource with PusherResource
