package io.paytouch.core.resources.returnorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.{ ArticleRecord, ReturnOrderProductRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReturnOrdersListProductsFSpec extends ReturnOrdersFSpec {

  abstract class ReturnOrdersFSpecContext extends ReturnOrderResourceFSpecContext {
    def assertResponseReturnOrderProduct(
        entity: ReturnOrderProduct,
        record: ReturnOrderProductRecord,
        product: ArticleRecord,
        currentQuantity: BigDecimal,
        options: Seq[VariantOptionWithType] = Seq.empty,
      ) = {
      entity.productId ==== record.productId
      product.id ==== record.productId
      entity.productName ==== product.name
      entity.productUnit ==== product.unit
      entity.quantity ==== record.quantity
      entity.currentQuantity ==== currentQuantity
      entity.options ==== options
    }
  }

  "GET /v1/return_orders.list_products?return_order_id=$" in {
    "if request has valid token" in {

      "if the return order exists" should {

        "with no parameters" should {
          "return the return order" in new ReturnOrdersFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product1London = Factory.productLocation(product1, london).create
            Factory.stock(product1London, Some(15)).create
            val product1Rome = Factory.productLocation(product1, rome).create
            Factory.stock(product1Rome, Some(1)).create

            val product2 = Factory.simpleProduct(merchant).create
            val product2London = Factory.productLocation(product2, london).create
            Factory.stock(product2London, Some(7)).create
            val product2Rome = Factory.productLocation(product2, rome).create
            Factory.stock(product2Rome, Some(7)).create

            val supplier = Factory.supplier(merchant).create
            val returnOrder = Factory.returnOrder(user, supplier, london).create
            val returnOrderProduct1 = Factory.returnOrderProduct(returnOrder, product1).create
            val returnOrderProduct2 =
              Factory.returnOrderProduct(returnOrder, product2, quantity = Some(5)).create

            Get(s"/v1/return_orders.list_products?return_order_id=${returnOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[ReturnOrderProduct]]].data
              assertResponseReturnOrderProduct(
                entities.find(_.productId == product1.id).get,
                returnOrderProduct1,
                product1,
                15,
              )
              assertResponseReturnOrderProduct(
                entities.find(_.productId == product2.id).get,
                returnOrderProduct2,
                product2,
                7,
              )
            }
          }
        }

        "with expand[]=options" should {
          "return the return order" in new ReturnOrdersFSpecContext {
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

            val variantLondon = Factory.productLocation(variantProduct, london).create
            val variantStockLondon = Factory.stock(variantLondon, Some(7)).create

            val supplier = Factory.supplier(merchant).create
            val returnOrder = Factory.returnOrder(user, supplier, london).create
            val returnOrderProduct1 = Factory.returnOrderProduct(returnOrder, simpleProduct).create
            val returnOrderProduct2 =
              Factory.returnOrderProduct(returnOrder, variantProduct, quantity = Some(5)).create

            Get(s"/v1/return_orders.list_products?return_order_id=${returnOrder.id}&expand[]=options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[ReturnOrderProduct]]].data
              assertResponseReturnOrderProduct(
                entities.find(_.productId == simpleProduct.id).get,
                returnOrderProduct1,
                simpleProduct,
                15,
              )
              assertResponseReturnOrderProduct(
                entities.find(_.productId == variantProduct.id).get,
                returnOrderProduct2,
                variantProduct,
                7,
                options = Seq(variantOptionWithType),
              )
            }
          }
        }
      }

      "if the return order does not belong to the merchant" should {
        "return empty list" in new ReturnOrdersFSpecContext {
          val competitor = Factory.merchant.create
          val locationCompetitor = Factory.location(competitor).create
          val supplierCompetitor = Factory.supplier(competitor).create
          val userCompetitor = Factory.user(competitor).create
          val returnOrderCompetitor =
            Factory.returnOrder(userCompetitor, supplierCompetitor, locationCompetitor).create

          Get(s"/v1/return_orders.list_products?return_order_id=${returnOrderCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[ReturnOrderProduct]]].data
            entities must beEmpty
          }
        }
      }

      "if the return order does not exist" should {
        "return empty list" in new ReturnOrdersFSpecContext {
          Get(s"/v1/return_orders.list_products?return_order_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[ReturnOrderProduct]]].data
            entities must beEmpty
          }
        }
      }
    }
  }
}
