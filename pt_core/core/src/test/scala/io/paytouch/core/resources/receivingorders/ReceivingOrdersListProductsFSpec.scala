package io.paytouch.core.resources.receivingorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReceivingOrdersListProductsFSpec extends ReceivingOrdersFSpec {

  abstract class ReceivingOrdersFSpecContext extends ReceivingOrderResourceFSpecContext {

    def assertResponseDetails(
        entity: ReceivingOrderProductDetails,
        product: ArticleRecord,
        quantityReceived: BigDecimal,
        currentQuantity: BigDecimal,
        receivedCost: MonetaryAmount,
        quantityOrdered: Option[BigDecimal],
        orderedCost: Option[MonetaryAmount],
        totalValue: MonetaryAmount,
        options: Seq[VariantOptionWithType] = Seq.empty,
      ) = {
      entity.productId ==== product.id
      entity.productName ==== product.name
      entity.productUnit ==== product.unit
      entity.quantityOrdered ==== quantityOrdered
      entity.quantityReceived ==== quantityReceived
      entity.currentQuantity ==== currentQuantity
      entity.orderedCost ==== orderedCost
      entity.receivedCost ==== receivedCost
      entity.totalValue ==== totalValue
      entity.options ==== options
    }
  }

  "GET /v1/receiving_orders.list_products?receiving_order_id=$" in {
    "if request has valid token" in {

      "if the receiving order has purchase or transfer orders" should {

        "with no parameters" should {
          "return the receiving order of a purchase order" in new ReceivingOrdersFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product1London = Factory.productLocation(product1, london).create
            val stock1 = Factory.stock(product1London, Some(15)).create

            val product2 = Factory.simpleProduct(merchant).create
            val product2London = Factory.productLocation(product2, london).create
            val stock2 = Factory.stock(product2London, Some(7)).create

            val purchaseOrder = Factory.purchaseOrder(merchant, london, user).create
            Factory
              .purchaseOrderProduct(purchaseOrder, product1, quantity = Some(10), costAmount = Some(3))
              .create
            Factory
              .purchaseOrderProduct(purchaseOrder, product2, quantity = Some(20), costAmount = Some(2))
              .create

            val receivingOrder = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder).create
            Factory
              .receivingOrderProduct(receivingOrder, product1, quantity = Some(4), costAmount = Some(1))
              .create
            Factory
              .receivingOrderProduct(receivingOrder, product2, quantity = Some(5), costAmount = Some(2))
              .create

            Get(s"/v1/receiving_orders.list_products?receiving_order_id=${receivingOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val details = responseAs[PaginatedApiResponse[Seq[ReceivingOrderProductDetails]]].data
              assertResponseDetails(
                details.find(_.productId == product1.id).get,
                product1,
                quantityReceived = 4,
                currentQuantity = 15,
                receivedCost = 1.$$$,
                quantityOrdered = Some(10),
                orderedCost = Some(3.$$$),
                totalValue = 4.$$$,
              )
              assertResponseDetails(
                details.find(_.productId == product2.id).get,
                product2,
                quantityReceived = 5,
                currentQuantity = 7,
                receivedCost = 2.$$$,
                quantityOrdered = Some(20),
                orderedCost = Some(2.$$$),
                totalValue = 10.$$$,
              )
            }
          }

          "return the receiving order of a transfer order" in new ReceivingOrdersFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product1London = Factory.productLocation(product1, london).create
            val stock1 = Factory.stock(product1London, Some(15)).create

            val product2 = Factory.simpleProduct(merchant).create
            val product2London = Factory.productLocation(product2, london).create
            val stock2 = Factory.stock(product2London, Some(7)).create

            val supplier = Factory.supplier(merchant).create

            val transferOrder = Factory.transferOrder(london, rome, user).create
            Factory
              .transferOrderProduct(transferOrder, product1, quantity = Some(10))
              .create
            Factory
              .transferOrderProduct(transferOrder, product2, quantity = Some(20))
              .create

            val receivingOrder = Factory.receivingOrderOfTransfer(london, user, transferOrder).create
            Factory
              .receivingOrderProduct(receivingOrder, product1, quantity = Some(4), costAmount = Some(1))
              .create
            Factory
              .receivingOrderProduct(receivingOrder, product2, quantity = Some(5), costAmount = Some(2))
              .create

            Get(s"/v1/receiving_orders.list_products?receiving_order_id=${receivingOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val details = responseAs[PaginatedApiResponse[Seq[ReceivingOrderProductDetails]]].data
              assertResponseDetails(
                details.find(_.productId == product1.id).get,
                product1,
                quantityReceived = 4,
                currentQuantity = 15,
                receivedCost = 1.$$$,
                quantityOrdered = Some(10),
                orderedCost = None,
                totalValue = 4.$$$,
              )
              assertResponseDetails(
                details.find(_.productId == product2.id).get,
                product2,
                quantityReceived = 5,
                currentQuantity = 7,
                receivedCost = 2.$$$,
                quantityOrdered = Some(20),
                orderedCost = None,
                totalValue = 10.$$$,
              )
            }
          }
        }

        "with expand[]=options" should {
          "return the receiving order" in new ReceivingOrdersFSpecContext {
            val simpleProduct = Factory.simpleProduct(merchant).create
            val simpleProductLondon = Factory.productLocation(simpleProduct, london).create
            val simpleStockLondon = Factory.stock(simpleProductLondon, Some(15)).create

            val template = Factory.templateProduct(merchant).create
            val variantProduct = Factory.variantProduct(merchant, template).create

            val variantType = Factory.variantOptionType(template).create
            val variantTypeOption1 = Factory.variantOption(template, variantType, "M").create
            Factory.productVariantOption(variantProduct, variantTypeOption1).create
            val variantOptionWithType = VariantOptionWithType(
              id = variantTypeOption1.id,
              name = variantTypeOption1.name,
              typeName = variantType.name,
              position = variantTypeOption1.position,
              typePosition = variantType.position,
            )

            val variantProductLondon = Factory.productLocation(variantProduct, london).create
            val variantStockLondon = Factory.stock(variantProductLondon, Some(7)).create

            val purchaseOrder = Factory.purchaseOrder(merchant, london, user).create
            Factory
              .purchaseOrderProduct(purchaseOrder, simpleProduct, quantity = Some(10), costAmount = Some(3))
              .create
            Factory
              .purchaseOrderProduct(purchaseOrder, variantProduct, quantity = Some(20), costAmount = Some(2))
              .create

            val receivingOrder = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder).create
            Factory
              .receivingOrderProduct(receivingOrder, simpleProduct, quantity = Some(4), costAmount = Some(1))
              .create
            Factory
              .receivingOrderProduct(receivingOrder, variantProduct, quantity = Some(5), costAmount = Some(2))
              .create

            Get(s"/v1/receiving_orders.list_products?receiving_order_id=${receivingOrder.id}&expand[]=options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val details = responseAs[PaginatedApiResponse[Seq[ReceivingOrderProductDetails]]].data
              assertResponseDetails(
                details.find(_.productId == simpleProduct.id).get,
                simpleProduct,
                quantityReceived = 4,
                currentQuantity = 15,
                receivedCost = 1.$$$,
                quantityOrdered = Some(10),
                orderedCost = Some(3.$$$),
                totalValue = 4.$$$,
              )
              assertResponseDetails(
                details.find(_.productId == variantProduct.id).get,
                variantProduct,
                quantityReceived = 5,
                currentQuantity = 7,
                receivedCost = 2.$$$,
                quantityOrdered = Some(20),
                orderedCost = Some(2.$$$),
                totalValue = 10.$$$,
                options = Seq(variantOptionWithType),
              )
            }
          }
        }
      }

      "if the receiving order is manual (no purchase or transfer orders)" should {

        "with no parameters" should {
          "return the receiving order" in new ReceivingOrdersFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product1London = Factory.productLocation(product1, london).create
            val stock1 = Factory.stock(product1London, Some(15)).create

            val product2 = Factory.simpleProduct(merchant).create
            val product2London = Factory.productLocation(product2, london).create
            val stock2 = Factory.stock(product2London, Some(7)).create

            val supplier = Factory.supplier(merchant).create

            val receivingOrder = Factory.receivingOrder(london, user).create
            Factory
              .receivingOrderProduct(receivingOrder, product1, quantity = Some(4), costAmount = Some(1))
              .create
            Factory
              .receivingOrderProduct(receivingOrder, product2, quantity = Some(5), costAmount = Some(2))
              .create

            Get(s"/v1/receiving_orders.list_products?receiving_order_id=${receivingOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val details = responseAs[PaginatedApiResponse[Seq[ReceivingOrderProductDetails]]].data
              assertResponseDetails(
                details.find(_.productId == product1.id).get,
                product1,
                quantityReceived = 4,
                currentQuantity = 15,
                receivedCost = 1.$$$,
                quantityOrdered = None,
                orderedCost = None,
                totalValue = 4.$$$,
              )
              assertResponseDetails(
                details.find(_.productId == product2.id).get,
                product2,
                quantityReceived = 5,
                currentQuantity = 7,
                receivedCost = 2.$$$,
                quantityOrdered = None,
                orderedCost = None,
                totalValue = 10.$$$,
              )
            }
          }
        }

        "with expand[]=options" should {
          "return the receiving order" in new ReceivingOrdersFSpecContext {
            val simpleProduct = Factory.simpleProduct(merchant).create
            val simpleProductLondon = Factory.productLocation(simpleProduct, london).create
            val simpleStockLondon = Factory.stock(simpleProductLondon, Some(15)).create

            val template = Factory.templateProduct(merchant).create
            val variantProduct = Factory.variantProduct(merchant, template).create

            val variantType = Factory.variantOptionType(template).create
            val variantTypeOption1 = Factory.variantOption(template, variantType, "M").create
            Factory.productVariantOption(variantProduct, variantTypeOption1).create
            val variantOptionWithType = VariantOptionWithType(
              id = variantTypeOption1.id,
              name = variantTypeOption1.name,
              typeName = variantType.name,
              position = variantTypeOption1.position,
              typePosition = variantType.position,
            )

            val variantProductLondon = Factory.productLocation(variantProduct, london).create
            val variantStockLondon = Factory.stock(variantProductLondon, Some(7)).create

            val supplier = Factory.supplier(merchant).create

            val receivingOrder = Factory.receivingOrder(london, user).create
            Factory
              .receivingOrderProduct(receivingOrder, simpleProduct, quantity = Some(4), costAmount = Some(1))
              .create
            Factory
              .receivingOrderProduct(receivingOrder, variantProduct, quantity = Some(5), costAmount = Some(2))
              .create

            Get(s"/v1/receiving_orders.list_products?receiving_order_id=${receivingOrder.id}&expand[]=options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val details = responseAs[PaginatedApiResponse[Seq[ReceivingOrderProductDetails]]].data
              assertResponseDetails(
                details.find(_.productId == simpleProduct.id).get,
                simpleProduct,
                quantityReceived = 4,
                currentQuantity = 15,
                receivedCost = 1.$$$,
                quantityOrdered = None,
                orderedCost = None,
                totalValue = 4.$$$,
              )
              assertResponseDetails(
                details.find(_.productId == variantProduct.id).get,
                variantProduct,
                quantityReceived = 5,
                currentQuantity = 7,
                receivedCost = 2.$$$,
                quantityOrdered = None,
                orderedCost = None,
                totalValue = 10.$$$,
                options = Seq(variantOptionWithType),
              )
            }
          }
        }
      }

      "if the receiving order does not exist or does not belong to the merchant" should {
        "return empty list" in new ReceivingOrdersFSpecContext {
          Get(s"/v1/receiving_orders.list_products?receiving_order_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[ReceivingOrderProductDetails]]].data
            entities must beEmpty
          }
        }
      }
    }
  }
}
