package io.paytouch.core.resources.purchaseorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums.{ NextNumberType, ScopeType }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class PurchaseOrdersCreateFSpec extends PurchaseOrdersFSpec {

  abstract class PurchaseOrdersCreateFSpecContext extends PurchaseOrderResourceFSpecContext {
    val nextNumberDao = daos.nextNumberDao

    def assertNextPurchaseOrderNumber(nextNumber: Int) = {
      val record = nextNumberDao
        .findByScopeAndType(Scope.fromMerchantId(merchant.id), NextNumberType.PurchaseOrder)
        .await
      record should beSome[NextNumberRecord]
      record.get.nextVal ==== nextNumber
    }
  }

  "POST /v1/purchase_orders.create" in {
    "if request has valid token" in {

      "create purchase order and return 201" in new PurchaseOrdersCreateFSpecContext {
        val newPurchaseOrderId = UUID.randomUUID
        val supplier = Factory.supplier(merchant).create

        val product = Factory.simpleProduct(merchant).create
        val purchaseOrderProduct = random[PurchaseOrderProductUpsertion].copy(productId = product.id)

        val creation = random[PurchaseOrderCreation].copy(
          supplierId = supplier.id,
          locationId = rome.id,
          products = Seq(purchaseOrderProduct),
        )

        Post(s"/v1/purchase_orders.create?purchase_order_id=$newPurchaseOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          responseAs[ApiResponse[PurchaseOrder]].data

          assertCreation(newPurchaseOrderId, creation)
          assertNextPurchaseOrderNumber(nextNumber = 2)
        }
      }
    }

    "if request has invalid supplier id" should {
      "return 404" in new PurchaseOrdersCreateFSpecContext {
        val newPurchaseOrderId = UUID.randomUUID
        val creation =
          random[PurchaseOrderCreation].copy(supplierId = UUID.randomUUID, locationId = rome.id, products = Seq.empty)
        Post(s"/v1/purchase_orders.create?purchase_order_id=$newPurchaseOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid location id" should {
      "return 404" in new PurchaseOrdersCreateFSpecContext {
        val newPurchaseOrderId = UUID.randomUUID
        val supplier = Factory.supplier(merchant).create
        val creation = random[PurchaseOrderCreation].copy(
          supplierId = supplier.id,
          locationId = UUID.randomUUID,
          products = Seq.empty,
        )
        Post(s"/v1/purchase_orders.create?purchase_order_id=$newPurchaseOrderId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }

      "if request has invalid product id" should {
        "return 404" in new PurchaseOrdersCreateFSpecContext {
          val newPurchaseOrderId = UUID.randomUUID
          val supplier = Factory.supplier(merchant).create

          val product = Factory.simpleProduct(merchant).create
          val purchaseOrderProduct = random[PurchaseOrderProductUpsertion]

          val creation = random[PurchaseOrderCreation].copy(
            supplierId = supplier.id,
            locationId = rome.id,
            products = Seq(purchaseOrderProduct),
          )

          Post(s"/v1/purchase_orders.create?purchase_order_id=$newPurchaseOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if request has invalid token" should {
        "be rejected" in new PurchaseOrdersCreateFSpecContext {
          val newPurchaseOrderId = UUID.randomUUID
          val creation = random[PurchaseOrderCreation]
          Post(s"/v1/purchase_orders.create?purchase_order_id=$newPurchaseOrderId", creation)
            .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
            rejection should beAnInstanceOf[AuthenticationFailedRejection]
          }
        }
      }
    }
  }
}
