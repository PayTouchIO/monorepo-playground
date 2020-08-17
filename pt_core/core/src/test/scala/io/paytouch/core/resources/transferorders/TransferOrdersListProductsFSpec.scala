package io.paytouch.core.resources.transferorders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.{ ArticleRecord, StockRecord, TransferOrderProductRecord }
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TransferOrdersListProductsFSpec extends TransferOrdersFSpec {

  abstract class TransferOrdersFSpecContext extends TransferOrderResourceFSpecContext {
    def assertResponseTransferOrderProduct(
        entity: TransferOrderProduct,
        record: TransferOrderProductRecord,
        product: ArticleRecord,
        fromStock: StockRecord,
        toStock: StockRecord,
        totalValue: MonetaryAmount,
        options: Seq[VariantOptionWithType] = Seq.empty,
      ) = {
      entity.productId ==== record.productId
      product.id ==== record.productId
      entity.productName ==== product.name
      entity.productUnit ==== product.unit
      entity.transferQuantity ==== record.quantity.getOrElse(0)
      entity.fromCurrentQuantity ==== fromStock.quantity
      entity.toCurrentQuantity ==== toStock.quantity
      entity.totalValue ==== totalValue
      entity.options ==== options
    }
  }

  "GET /v1/transfer_orders.list_products?transfer_order_id=$" in {
    "if request has valid token" in {

      "if the transfer order exists" should {

        "with no parameters" in {
          "return the transfer order" in new TransferOrdersFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product1London = Factory.productLocation(product1, london, costAmount = Some(10)).create
            val stock1London = Factory.stock(product1London, Some(15)).create
            val product1Rome = Factory.productLocation(product1, rome, costAmount = Some(8)).create
            val stock1Rome = Factory.stock(product1Rome, Some(27)).create

            val product2 = Factory.simpleProduct(merchant).create
            val product2London = Factory.productLocation(product2, london, costAmount = Some(20)).create
            val stock2London = Factory.stock(product2London, Some(15)).create
            val product2Rome = Factory.productLocation(product2, rome, costAmount = Some(16)).create
            val stock2Rome = Factory.stock(product2Rome, Some(27)).create

            val transferOrder = Factory.transferOrder(rome, london, user).create
            val transferOrderProduct1 =
              Factory.transferOrderProduct(transferOrder, product1, quantity = Some(3)).create
            val transferOrderProduct2 =
              Factory.transferOrderProduct(transferOrder, product2, quantity = Some(5)).create

            Get(s"/v1/transfer_orders.list_products?transfer_order_id=${transferOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[TransferOrderProduct]]].data
              assertResponseTransferOrderProduct(
                entities.find(_.productId == product1.id).get,
                transferOrderProduct1,
                product1,
                stock1Rome,
                stock1London,
                30.$$$,
              )
              assertResponseTransferOrderProduct(
                entities.find(_.productId == product2.id).get,
                transferOrderProduct2,
                product2,
                stock2Rome,
                stock2London,
                100.$$$,
              )
            }
          }
        }

        "with expand[]=options" in {
          "return the transfer order" in new TransferOrdersFSpecContext {
            val simpleProduct = Factory.simpleProduct(merchant).create
            val simpleProductLondon =
              Factory.productLocation(simpleProduct, london, costAmount = Some(10)).create
            val simpleStockLondon = Factory.stock(simpleProductLondon, Some(15)).create
            val simpleProductRome = Factory.productLocation(simpleProduct, rome, costAmount = Some(8)).create
            val simpleStockRome = Factory.stock(simpleProductRome, Some(27)).create

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

            val variantProductLondon =
              Factory.productLocation(variantProduct, london, costAmount = Some(20)).create
            val variantStockLondon = Factory.stock(variantProductLondon, Some(15)).create
            val variantProductRome =
              Factory.productLocation(variantProduct, rome, costAmount = Some(16)).create
            val variantStockRome = Factory.stock(variantProductRome, Some(27)).create

            val transferOrder = Factory.transferOrder(rome, london, user).create
            val transferOrderSimpleProduct =
              Factory.transferOrderProduct(transferOrder, simpleProduct, quantity = Some(3)).create
            val transferOrderVariantProduct =
              Factory.transferOrderProduct(transferOrder, variantProduct, quantity = Some(5)).create

            Get(s"/v1/transfer_orders.list_products?transfer_order_id=${transferOrder.id}&expand[]=options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[TransferOrderProduct]]].data
              assertResponseTransferOrderProduct(
                entities.find(_.productId == simpleProduct.id).get,
                transferOrderSimpleProduct,
                simpleProduct,
                simpleStockRome,
                simpleStockLondon,
                30.$$$,
              )
              assertResponseTransferOrderProduct(
                entities.find(_.productId == variantProduct.id).get,
                transferOrderVariantProduct,
                variantProduct,
                variantStockRome,
                variantStockLondon,
                100.$$$,
                options = Seq(variantOptionWithType),
              )
            }
          }
        }
      }

      "if the transfer order does not belong to the merchant" should {
        "return empty list" in new TransferOrdersFSpecContext {
          val competitor = Factory.merchant.create
          val fromLocationCompetitor = Factory.location(competitor).create
          val toLocationCompetitor = Factory.location(competitor).create
          val userCompetitor = Factory.user(competitor).create
          val transferOrderCompetitor =
            Factory.transferOrder(fromLocationCompetitor, toLocationCompetitor, userCompetitor).create

          Get(s"/v1/transfer_orders.list_products?transfer_order_id=${transferOrderCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[TransferOrderProduct]]].data
            entities must beEmpty
          }
        }
      }

      "if the transfer order does not exist" should {
        "return empty list" in new TransferOrdersFSpecContext {
          Get(s"/v1/transfer_orders.list_products?transfer_order_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[TransferOrderProduct]]].data
            entities must beEmpty
          }
        }
      }
    }
  }
}
