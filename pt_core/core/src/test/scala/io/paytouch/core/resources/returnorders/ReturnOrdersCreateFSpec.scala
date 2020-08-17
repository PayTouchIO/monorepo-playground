package io.paytouch.core.resources.returnorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums.{ NextNumberType, ScopeType }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class ReturnOrdersCreateFSpec extends ReturnOrdersFSpec {
  abstract class ReturnOrdersCreateFSpecContext extends ReturnOrderResourceFSpecContext {
    val supplier = Factory.supplier(merchant).create
    val nextNumberDao = daos.nextNumberDao

    def assertNextReturnOrderNumber(nextNumber: Int) = {
      val record =
        nextNumberDao.findByScopeAndType(Scope.fromMerchantId(merchant.id), NextNumberType.ReturnOrder).await
      record should beSome[NextNumberRecord]
      record.get.nextVal ==== nextNumber
    }
  }

  "POST /v1/return_orders.create" in {
    "if request has valid token" in {

      "if all the ids are valid" should {
        "create return order and return 201" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val creation = random[ReturnOrderCreation].copy(
            supplierId = supplier.id,
            locationId = london.id,
            userId = user.id,
            products = Seq(returnOrderProduct),
          )

          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[ReturnOrder]].data
            assertCreation(newReturnOrderId, creation)
            assertNextReturnOrderNumber(nextNumber = 2)
          }
        }
      }

      "if referencing an existing purchase order id" should {
        "create receiving order and return 201" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val purchaseOrder = Factory.purchaseOrder(merchant, rome, user).create

          val creation = random[ReturnOrderCreation].copy(
            supplierId = supplier.id,
            locationId = london.id,
            purchaseOrderId = Some(purchaseOrder.id),
            userId = user.id,
            products = Seq(returnOrderProduct),
          )

          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            responseAs[ApiResponse[ReturnOrder]].data
            assertCreation(newReturnOrderId, creation)
          }
        }
      }

      "if request has invalid purchase order id" should {
        "return 404" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID
          val creation = random[ReturnOrderCreation].copy(
            supplierId = supplier.id,
            locationId = london.id,
            purchaseOrderId = Some(UUID.randomUUID),
            userId = user.id,
            products = Seq.empty,
          )
          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if location id is not valid" should {
        "return 404" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val creation = random[ReturnOrderCreation].copy(
            supplierId = supplier.id,
            userId = user.id,
            products = Seq(returnOrderProduct),
          )

          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if supplier id is not valid" should {
        "return 404" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val creation =
            random[ReturnOrderCreation].copy(locationId = rome.id, userId = user.id, products = Seq(returnOrderProduct))

          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user id is not valid" should {
        "return 404" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID

          val product = Factory.simpleProduct(merchant).create
          val returnOrderProduct = random[ReturnOrderProductUpsertion].copy(productId = product.id)

          val creation = random[ReturnOrderCreation].copy(
            supplierId = supplier.id,
            locationId = london.id,
            products = Seq(returnOrderProduct),
          )

          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the product id is not valid" should {
        "return 404" in new ReturnOrdersCreateFSpecContext {
          val newReturnOrderId = UUID.randomUUID
          val returnOrderProduct = random[ReturnOrderProductUpsertion]

          val creation = random[ReturnOrderCreation].copy(
            supplierId = supplier.id,
            locationId = london.id,
            userId = user.id,
            products = Seq(returnOrderProduct),
          )

          Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ReturnOrdersCreateFSpecContext {
        val newReturnOrderId = UUID.randomUUID
        val creation = random[ReturnOrderCreation]
        Post(s"/v1/return_orders.create?return_order_id=$newReturnOrderId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
