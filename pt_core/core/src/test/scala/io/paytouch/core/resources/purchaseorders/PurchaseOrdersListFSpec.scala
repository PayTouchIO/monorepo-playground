package io.paytouch.core.resources.purchaseorders

import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities.{ PaginatedApiResponse, PurchaseOrder => PurchaseOrderEntity }
import io.paytouch.core.utils.{ Formatters, UtcTime, FixtureDaoFactory => Factory }

class PurchaseOrdersListFSpec extends PurchaseOrdersFSpec {

  trait PurchaseOrdersListFspecContext extends PurchaseOrderResourceFSpecContext {
    val now = UtcTime.now
  }

  "GET /v1/purchase_orders.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all purchase orders ordered by created_at desc" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val purchaseOrder1 = Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now)).create
          val purchaseOrder2 =
            Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now.minusDays(1))).create
          val purchaseOrder3 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(2))).create
          val purchaseOrder4 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(3))).create

          Get("/v1/purchase_orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id, purchaseOrder2.id, purchaseOrder3.id, purchaseOrder4.id)
            assertResponse(entities.data.find(_.id == purchaseOrder1.id).get, purchaseOrder1)
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
            assertResponse(entities.data.find(_.id == purchaseOrder3.id).get, purchaseOrder3)
            assertResponse(entities.data.find(_.id == purchaseOrder4.id).get, purchaseOrder4)
          }
        }
      }

      "with location_id filter" should {
        "return a paginated list of all purchase orders filtered by location_id" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val purchaseOrder1 = Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now)).create
          val purchaseOrder2 =
            Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now.minusDays(1))).create
          val purchaseOrder3 =
            Factory.purchaseOrderWithSupplier(supplier2, london, user, overrideNow = Some(now.minusDays(2))).create
          val purchaseOrder4 =
            Factory.purchaseOrderWithSupplier(supplier2, london, user, overrideNow = Some(now.minusDays(3))).create

          Get(s"/v1/purchase_orders.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id, purchaseOrder2.id)
            assertResponse(entities.data.find(_.id == purchaseOrder1.id).get, purchaseOrder1)
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
          }
        }
      }

      "with supplier_id filter" should {
        "return a paginated list of all purchase orders filtered by supplier_id" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val purchaseOrder1 = Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now)).create
          val purchaseOrder2 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(1))).create
          val purchaseOrder3 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(2))).create
          val purchaseOrder4 =
            Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now.minusDays(3))).create

          Get(s"/v1/purchase_orders.list?supplier_id=${supplier2.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder2.id, purchaseOrder3.id)
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
            assertResponse(entities.data.find(_.id == purchaseOrder3.id).get, purchaseOrder3)
          }
        }
      }

      "with status filter" should {
        "return a paginated list of all purchase orders filtered by status" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val purchaseOrder1 =
            Factory
              .purchaseOrderWithSupplier(supplier1, rome, user, status = Some(ReceivingObjectStatus.Partial))
              .create
          val purchaseOrder2 =
            Factory
              .purchaseOrderWithSupplier(supplier2, rome, user, status = Some(ReceivingObjectStatus.Completed))
              .create
          val purchaseOrder3 =
            Factory
              .purchaseOrderWithSupplier(supplier2, rome, user, status = Some(ReceivingObjectStatus.Created))
              .create
          val purchaseOrder4 =
            Factory
              .purchaseOrderWithSupplier(supplier1, rome, user, status = Some(ReceivingObjectStatus.Completed))
              .create

          Get(s"/v1/purchase_orders.list?status=created").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder3.id)
            assertResponse(entities.data.find(_.id == purchaseOrder3.id).get, purchaseOrder3)
          }
        }
      }

      "with from filter" should {
        "return a paginated list of all purchase orders filtered by from date" in new PurchaseOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val purchaseOrder1 =
            Factory.purchaseOrder(merchant, rome, user, overrideNow = Some(yesterday.minusDays(10))).create
          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, overrideNow = Some(yesterday.plusDays(10))).create

          Get(s"/v1/purchase_orders.list?from=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder2.id)
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
          }
        }
      }

      "with to filter" should {
        "return a paginated list of all purchase orders filtered by to date" in new PurchaseOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val purchaseOrder1 =
            Factory.purchaseOrder(merchant, rome, user, overrideNow = Some(yesterday.minusDays(10))).create
          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, overrideNow = Some(yesterday.plusDays(10))).create

          Get(s"/v1/purchase_orders.list?to=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id)
            assertResponse(entities.data.find(_.id == purchaseOrder1.id).get, purchaseOrder1)
          }
        }
      }

      "with q filter" should {
        "return a paginated list of all purchase orders filtered by a given query on purchase order number" in new PurchaseOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val purchaseOrder1 = Factory.purchaseOrder(merchant, rome, user).create
          val purchaseOrder2 = Factory.purchaseOrder(merchant, rome, user).create

          Get(s"/v1/purchase_orders.list?q=2").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder2.id)
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
          }
        }
      }

      "with view filter" should {
        "return a paginated list of all purchase orders filtered by view=complete" in new PurchaseOrdersListFspecContext {
          val purchaseOrder1 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Receiving)).create
          val purchaseOrder3 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Partial)).create
          val purchaseOrder4 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Created)).create

          Get(s"/v1/purchase_orders.list?view=complete").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id)
            assertResponse(entities.data.find(_.id == purchaseOrder1.id).get, purchaseOrder1)
          }
        }

        "return a paginated list of all purchase orders filtered by view=incomplete" in new PurchaseOrdersListFspecContext {
          val purchaseOrder1 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Receiving)).create
          val purchaseOrder3 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Partial)).create
          val purchaseOrder4 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Created)).create

          Get(s"/v1/purchase_orders.list?view=incomplete").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id).sorted ==== Seq(purchaseOrder2.id, purchaseOrder3.id, purchaseOrder4.id).sorted
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
            assertResponse(entities.data.find(_.id == purchaseOrder3.id).get, purchaseOrder3)
            assertResponse(entities.data.find(_.id == purchaseOrder4.id).get, purchaseOrder4)
          }
        }

        "return a list of all purchase orders filtered by view=available_for_return" in new PurchaseOrdersListFspecContext {
          val product = Factory.simpleProduct(merchant).create
          val productRome = Factory.productLocation(product, rome).create
          val supplier = Factory.supplier(merchant).create

          // Case 1: Items not received yet - not included
          val purchaseOrder1 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrderProduct1 = Factory.purchaseOrderProduct(purchaseOrder1, product, quantity = Some(20)).create

          // Case 2: No items returned - included
          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder2Product1 = Factory.purchaseOrderProduct(purchaseOrder2, product, quantity = Some(20)).create

          val receivingOrder1 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder2).create
          val receivingOrder1Product1 =
            Factory.receivingOrderProduct(receivingOrder1, product, quantity = Some(15)).create

          // Case 3: Items partially returned - included
          val purchaseOrder3 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder3Product1 = Factory.purchaseOrderProduct(purchaseOrder3, product, quantity = Some(20)).create

          val receivingOrder2 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder3).create
          val receivingOrder2product1 =
            Factory.receivingOrderProduct(receivingOrder2, product, quantity = Some(15)).create

          val returnOrder1 = Factory.returnOrder(user, supplier, london, Some(purchaseOrder3)).create
          val returnOrder1Product1 =
            Factory
              .returnOrderProduct(returnOrder1, product, quantity = Some(5))
              .create

          // Case 4: All items returned - not included
          val purchaseOrder4 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder4Product1 = Factory.purchaseOrderProduct(purchaseOrder4, product, quantity = Some(20)).create

          val receivingOrder3 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder4).create
          val receivingOrder3product1 =
            Factory.receivingOrderProduct(receivingOrder3, product, quantity = Some(15)).create

          val returnOrder2 = Factory.returnOrder(user, supplier, london, Some(purchaseOrder4)).create
          val returnOrder2Product1 =
            Factory
              .returnOrderProduct(returnOrder2, product, quantity = Some(15))
              .create

          Get(s"/v1/purchase_orders.list?view=available_for_return").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id).sorted ==== Seq(purchaseOrder2.id, purchaseOrder3.id).sorted
            assertResponse(entities.data.find(_.id == purchaseOrder2.id).get, purchaseOrder2)
            assertResponse(entities.data.find(_.id == purchaseOrder3.id).get, purchaseOrder3)
          }
        }
      }

      "with expand[]=receiving_orders" should {
        "return a paginated list of all purchase orders filtered by a given query" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val yesterday = UtcTime.now.minusDays(1).withZoneSameInstant(genZoneId.instance)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val purchaseOrder1 = Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now)).create
          val receivingOrder11 = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder1).create
          val receivingOrder12 = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder1).create

          val purchaseOrder2 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(1))).create

          Get(s"/v1/purchase_orders.list?expand[]=receiving_orders")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id, purchaseOrder2.id)
            assertResponse(
              entities.data.find(_.id == purchaseOrder1.id).get,
              purchaseOrder1,
              receivingOrders = Some(Seq(receivingOrder11, receivingOrder12)),
            )
            assertResponse(
              entities.data.find(_.id == purchaseOrder2.id).get,
              purchaseOrder2,
              receivingOrders = Some(Seq.empty),
            )
          }
        }
      }

      "with expand[]=supplier" should {
        "return a paginated list of all purchase orders filtered by a given query" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val purchaseOrder1 = Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now)).create
          val purchaseOrder2 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(1))).create

          Get(s"/v1/purchase_orders.list?expand[]=supplier").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id, purchaseOrder2.id)
            assertResponse(
              entities.data.find(_.id == purchaseOrder1.id).get,
              purchaseOrder1,
              supplier = Some(supplier1),
            )
            assertResponse(
              entities.data.find(_.id == purchaseOrder2.id).get,
              purchaseOrder2,
              supplier = Some(supplier2),
            )
          }
        }
      }

      "with expand[]=ordered_products_count" should {
        "return a paginated list of all purchase orders filtered by a given query" in new PurchaseOrdersListFspecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create

          val purchaseOrder1 = Factory.purchaseOrder(merchant, rome, user, overrideNow = Some(now)).create
          Factory.purchaseOrderProduct(purchaseOrder1, product1, quantity = Some(10)).create
          Factory.purchaseOrderProduct(purchaseOrder1, product2, quantity = Some(5)).create

          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, overrideNow = Some(now.minusDays(1))).create
          Factory.purchaseOrderProduct(purchaseOrder2, product1, quantity = Some(100)).create

          Get(s"/v1/purchase_orders.list?expand[]=ordered_products_count")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id) ==== Seq(purchaseOrder1.id, purchaseOrder2.id)
            assertResponse(
              entities.data.find(_.id == purchaseOrder1.id).get,
              purchaseOrder1,
              orderedProductsCount = Some(15),
            )
            assertResponse(
              entities.data.find(_.id == purchaseOrder2.id).get,
              purchaseOrder2,
              orderedProductsCount = Some(100),
            )
          }
        }
      }

      "with expand[]=received_products_count" should {
        "return a paginated list of all purchase orders filtered by a given query" in new PurchaseOrdersListFspecContext {
          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create

          val purchaseOrder1 = Factory.purchaseOrderWithSupplier(supplier1, rome, user, overrideNow = Some(now)).create
          val receivingOrder11 = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder1).create
          Factory.receivingOrderProduct(receivingOrder11, product1, quantity = Some(23)).create
          Factory.receivingOrderProduct(receivingOrder11, product2, quantity = Some(2)).create
          val receivingOrder12 = Factory.receivingOrderOfPurchaseOrder(rome, user, purchaseOrder1).create
          Factory.receivingOrderProduct(receivingOrder12, product1, quantity = Some(5)).create

          val purchaseOrder2 =
            Factory.purchaseOrderWithSupplier(supplier2, rome, user, overrideNow = Some(now.minusDays(1))).create
          Factory.purchaseOrderProduct(purchaseOrder2, product1, quantity = Some(100)).create

          Get(s"/v1/purchase_orders.list?expand[]=received_products_count")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id).sorted ==== Seq(purchaseOrder1.id, purchaseOrder2.id).sorted
            assertResponse(
              entities.data.find(_.id == purchaseOrder1.id).get,
              purchaseOrder1,
              receivedProductsCount = Some(30),
            )
            assertResponse(
              entities.data.find(_.id == purchaseOrder2.id).get,
              purchaseOrder2,
              receivedProductsCount = Some(0),
            )
          }
        }
      }

      "with expand[]=returned_products_count" should {
        "return a paginated list of all purchase orders filtered by a given query" in new PurchaseOrdersListFspecContext {
          val supplier = Factory.supplier(merchant).create
          val product = Factory.simpleProduct(merchant).create

          val purchaseOrder1 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder1Product1 = Factory.purchaseOrderProduct(purchaseOrder1, product, quantity = Some(20)).create

          val purchaseOrder2 =
            Factory.purchaseOrder(merchant, rome, user, status = Some(ReceivingObjectStatus.Completed)).create
          val purchaseOrder2Product1 = Factory.purchaseOrderProduct(purchaseOrder2, product, quantity = Some(20)).create

          val returnOrder1 = Factory.returnOrder(user, supplier, london, Some(purchaseOrder2)).create
          val returnOrder1Product1 =
            Factory
              .returnOrderProduct(returnOrder1, product, quantity = Some(5))
              .create

          Get(s"/v1/purchase_orders.list?expand[]=returned_products_count")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[PurchaseOrderEntity]]]
            entities.data.map(_.id).sorted ==== Seq(purchaseOrder1.id, purchaseOrder2.id).sorted
            assertResponse(
              entities.data.find(_.id == purchaseOrder1.id).get,
              purchaseOrder1,
              returnedProductsCount = Some(0),
            )
            assertResponse(
              entities.data.find(_.id == purchaseOrder2.id).get,
              purchaseOrder2,
              returnedProductsCount = Some(5),
            )
          }
        }
      }
    }
  }
}
