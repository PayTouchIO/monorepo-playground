package io.paytouch.ordering.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.{ CoreEmbeddedError, CoreEmbeddedErrorResponse }
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.errors.{ ClientError, ProductOutOfStock }
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ CommonArbitraries, FixtureDaoFactory => Factory, MockedRestApi }

class CartSyncServiceSpec extends ServiceDaoSpec with CommonArbitraries {

  abstract class CartSyncServiceSpecContext extends ServiceDaoSpecContext {
    val service = MockedRestApi.cartSyncService

    implicit val storeContext = StoreContext.fromRecord(londonStore)

    val coreItemTaxRateId = UUID.randomUUID
    val coreTaxRateId = UUID.randomUUID
    val coreModifierOptionId = UUID.randomUUID
    val coreVariantOptionId = UUID.randomUUID

    val cartRecord = Factory.cart(londonStore).create
    Factory.cartTaxRate(cartRecord, taxRateId = Some(coreTaxRateId)).create
    val cartItem = Factory.cartItem(cartRecord).create
    Factory.cartItemTaxRate(cartItem, taxRateId = Some(coreItemTaxRateId)).create
    Factory.cartItemModifierOption(cartItem, modifierOptionId = Some(coreModifierOptionId)).create
    Factory.cartItemVariantOption(cartItem, variantOptionId = Some(coreVariantOptionId))

    lazy val order = randomOrder()
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordOrder(order)
  }

  "CartSyncService" in {
    "sync" should {
      "successful response" should {
        "return a successful upsertion result" in new CartSyncServiceSpecContext {
          service.sync(cartRecord, (_, upsertion) => upsertion).await.success
        }

        "with an existing order" should {
          "return a successful upsertion result" in new CartSyncServiceSpecContext {
            val cartRecordWithExistingOrder = cartRecord.copy(
              orderId = Some(order.id),
            )

            service.sync(cartRecordWithExistingOrder, (_, upsertion) => upsertion).await.success
          }
        }
      }
    }

    "validatedSync" should {
      "successful response" should {
        "return a successful upsertion result" in new CartSyncServiceSpecContext {
          service.validatedSync(cartRecord, (_, upsertion) => upsertion).await.success
        }

        "with an existing order" should {
          "return a successful upsertion result" in new CartSyncServiceSpecContext {
            val cartRecordWithExistingOrder = cartRecord.copy(
              orderId = Some(order.id),
            )

            service.validatedSync(cartRecordWithExistingOrder, (_, upsertion) => upsertion).await.success
          }
        }
      }

      "error response" should {
        "return a parsed error response for ProductOutOfStock error" in new CartSyncServiceSpecContext {
          val response = CoreEmbeddedErrorResponse(
            errors = Seq(
              CoreEmbeddedError(
                code = "ProductOutOfStock",
                message = "Product is out of stock",
                values = Seq(
                  "03d3fcd7-96d1-3868-abee-ab3d0cad7bb4",
                ),
              ),
            ),
            objectWithErrors = None,
          )

          PtCoreStubData.stubValidatedSyncErrorResponse(response)
          val error = service.validatedSync(cartRecord, (_, upsertion) => upsertion).await.failures
          error ==== Seq(ProductOutOfStock(UUID.fromString("03d3fcd7-96d1-3868-abee-ab3d0cad7bb4")))
        }

        "return a ClientError response for unhandled errors" in new CartSyncServiceSpecContext {
          val response = CoreEmbeddedErrorResponse(
            errors = Seq(
              CoreEmbeddedError(
                code = "SomethingWentWrong",
                message = "Something went wrong",
                values = Seq(
                  "NotAUuid",
                ),
              ),
            ),
            objectWithErrors = None,
          )

          PtCoreStubData.stubValidatedSyncErrorResponse(response)

          val error = service.validatedSync(cartRecord, (_, upsertion) => upsertion).await.failures
          error ==== Seq(ClientError("core.stub.orders.validated_sync", response))
        }
      }
    }
  }
}
