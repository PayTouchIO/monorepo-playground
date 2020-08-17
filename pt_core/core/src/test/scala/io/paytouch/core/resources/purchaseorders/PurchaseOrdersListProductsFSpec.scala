package io.paytouch.core.resources.purchaseorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.{
  ArticleRecord,
  PurchaseOrderProductRecord,
  ReceivingOrderProductRecord,
  ReturnOrderProductRecord,
}
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.entities.MonetaryAmount._

class PurchaseOrdersListProductsFSpec extends PurchaseOrdersFSpec {
  abstract class PurchaseOrdersFSpecContext extends PurchaseOrderResourceFSpecContext {
    def assertResponsePurchaseOrderProduct(
        entity: PurchaseOrderProduct,
        record: PurchaseOrderProductRecord,
        product: ArticleRecord,
        currentQuantity: BigDecimal,
        receivedQuantityAndCostAverage: (BigDecimal, Option[MonetaryAmount]) = (0, None),
        returnOrderProducts: Seq[ReturnOrderProductRecord] = Seq.empty,
        options: Seq[VariantOptionWithType] = Seq.empty,
      ) = {
      entity.productId ==== record.productId
      product.id ==== record.productId
      entity.productName ==== product.name
      entity.productUnit ==== product.unit
      entity.quantityOrdered ==== record.quantity.getOrElse[BigDecimal](0)
      entity.quantityReceived ==== Some(receivedQuantityAndCostAverage._1)
      entity.quantityReturned ==== Some(returnOrderProducts.flatMap(_.quantity).sum)
      entity.currentQuantity ==== currentQuantity
      entity.averageCost ==== MonetaryAmount.extract(product.averageCostAmount, currency)
      entity.orderedCost ==== MonetaryAmount.extract(record.costAmount, currency)
      entity.receivedCost ==== receivedQuantityAndCostAverage._2
      entity.options ==== options
    }
  }

  "GET /v1/purchase_orders.list_products?purchase_order_id=$" in {
    "if request has valid token" in {

      "if the purchase order exists" should {

        "with no parameters" should {
          "return the purchase order" in new PurchaseOrdersFSpecContext {
            implicit val u: UserContext = userContext
            val product1 = Factory.simpleProduct(merchant).create
            val product1London = Factory.productLocation(product1, london).create
            val stock1London = Factory.stock(product1London, Some(15)).create
            val product1Rome = Factory.productLocation(product1, rome).create
            val stock1Rome = Factory.stock(product1Rome, Some(150)).create

            val product2 = Factory.simpleProduct(merchant).create
            val product2London = Factory.productLocation(product2, london).create
            val stock2 = Factory.stock(product2London, Some(7)).create

            val purchaseOrder = Factory.purchaseOrder(merchant, london, user).create
            val purchaseOrderProduct1 = Factory.purchaseOrderProduct(purchaseOrder, product1).create
            val purchaseOrderProduct2 =
              Factory.purchaseOrderProduct(purchaseOrder, product2, quantity = Some(5)).create

            val receivingOrder1 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder).create
            val receivingOrder1Product1 =
              Factory.receivingOrderProduct(receivingOrder1, product2, quantity = Some(3), costAmount = Some(3)).create
            val receivingOrder2 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder).create
            val receivingOrder2Product2 =
              Factory.receivingOrderProduct(receivingOrder2, product2, quantity = Some(3), costAmount = Some(4)).create

            Get(s"/v1/purchase_orders.list_products?purchase_order_id=${purchaseOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[PurchaseOrderProduct]]].data
              assertResponsePurchaseOrderProduct(
                entity = entities.find(_.productId == product1.id).get,
                record = purchaseOrderProduct1,
                product = product1,
                currentQuantity = 15,
              )
              assertResponsePurchaseOrderProduct(
                entity = entities.find(_.productId == product2.id).get,
                record = purchaseOrderProduct2,
                product = product2,
                currentQuantity = 7,
                receivedQuantityAndCostAverage = (6, Some(3.5.$$$)),
              )
            }
          }
        }

        "with expand[]=options" should {
          "return the purchase order" in new PurchaseOrdersFSpecContext {
            val simpleProduct = Factory.simpleProduct(merchant).create
            val simpleProductLondon = Factory.productLocation(simpleProduct, london).create
            val simpleStockLondon = Factory.stock(simpleProductLondon, Some(15)).create
            val simpleProductRome = Factory.productLocation(simpleProduct, rome).create
            val simpleStockRome = Factory.stock(simpleProductRome, Some(150)).create

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
            val variantStockLondon = Factory.stock(variantProductLondon, Some(6)).create

            val supplier = Factory.supplier(merchant).create
            val purchaseOrder = Factory.purchaseOrderWithSupplier(supplier, london, user).create
            val purchaseOrderSimpleProduct = Factory.purchaseOrderProduct(purchaseOrder, simpleProduct).create
            val purchaseOrderVariantProduct =
              Factory.purchaseOrderProduct(purchaseOrder, variantProduct, quantity = Some(5)).create

            val receivingOrder1 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder).create
            val receivingOrder1Product1 =
              Factory
                .receivingOrderProduct(receivingOrder1, variantProduct, quantity = Some(3), costAmount = Some(3))
                .create
            val receivingOrder2 = Factory.receivingOrderOfPurchaseOrder(london, user, purchaseOrder).create
            val receivingOrder2Product2 =
              Factory
                .receivingOrderProduct(receivingOrder2, variantProduct, quantity = Some(3), costAmount = Some(4))
                .create

            val returnOrder1 = Factory.returnOrder(user, supplier, london, Some(purchaseOrder)).create
            val returnOrder1Product1 =
              Factory
                .returnOrderProduct(returnOrder1, variantProduct, quantity = Some(1))
                .create

            Get(s"/v1/purchase_orders.list_products?purchase_order_id=${purchaseOrder.id}&expand[]=options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[PurchaseOrderProduct]]].data
              assertResponsePurchaseOrderProduct(
                entity = entities.find(_.productId == simpleProduct.id).get,
                record = purchaseOrderSimpleProduct,
                product = simpleProduct,
                currentQuantity = 15,
              )
              assertResponsePurchaseOrderProduct(
                entity = entities.find(_.productId == variantProduct.id).get,
                record = purchaseOrderVariantProduct,
                product = variantProduct,
                currentQuantity = 6,
                receivedQuantityAndCostAverage = (6, Some(3.5.$$$)),
                returnOrderProducts = Seq(returnOrder1Product1),
                options = Seq(variantOptionWithType),
              )
            }
          }
        }
      }

      "if the purchase order does not belong to the merchant" should {
        "return empty list" in new PurchaseOrdersFSpecContext {
          val competitor = Factory.merchant.create
          val locationCompetitor = Factory.location(competitor).create
          val userCompetitor = Factory.user(competitor).create
          val purchaseOrderCompetitor =
            Factory.purchaseOrder(competitor, locationCompetitor, userCompetitor).create

          Get(s"/v1/purchase_orders.list_products?purchase_order_id=${purchaseOrderCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[PurchaseOrderProduct]]].data
            entities must beEmpty
          }
        }
      }

      "if the purchase order does not exist" should {
        "return empty list" in new PurchaseOrdersFSpecContext {
          Get(s"/v1/purchase_orders.list_products?purchase_order_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[PurchaseOrderProduct]]].data
            entities must beEmpty
          }
        }
      }
    }
  }
}
