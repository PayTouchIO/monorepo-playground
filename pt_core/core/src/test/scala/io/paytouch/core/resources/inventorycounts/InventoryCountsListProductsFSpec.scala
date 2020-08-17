package io.paytouch.core.resources.inventorycounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.{ ArticleRecord, InventoryCountProductRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class InventoryCountsListProductsFSpec extends InventoryCountsFSpec {

  abstract class InventoryCountsFSpecContext extends InventoryCountResourceFSpecContext {
    def assertResponseInventoryCountProduct(
        entity: InventoryCountProduct,
        record: InventoryCountProductRecord,
        product: ArticleRecord,
        options: Seq[VariantOptionWithType] = Seq.empty,
      ) = {
      entity.productId ==== record.productId
      product.id ==== record.productId
      entity.productName ==== product.name
      entity.productUnit ==== product.unit
      entity.expectedQuantity ==== record.expectedQuantity.orElse[BigDecimal](Some(0))
      entity.countedQuantity ==== record.countedQuantity.orElse[BigDecimal](Some(0))
      entity.value ==== MonetaryAmount.extract(record.valueAmount, currency)
      entity.options ==== options
    }
  }

  "GET /v1/inventory_counts.list_products?inventory_count_id=$" in {
    "if request has valid token" in {

      "if the inventory count exists" should {

        "with no parameters" should {
          "return the inventory count" in new InventoryCountsFSpecContext {
            val product1 = Factory.simpleProduct(merchant).create
            val product2 = Factory.simpleProduct(merchant).create

            val inventoryCount = Factory.inventoryCount(london, user).create
            val inventoryCountProduct1 = Factory.inventoryCountProduct(inventoryCount, product1).create
            val inventoryCountProduct2 = Factory.inventoryCountProduct(inventoryCount, product2).create

            Get(s"/v1/inventory_counts.list_products?inventory_count_id=${inventoryCount.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[InventoryCountProduct]]].data
              assertResponseInventoryCountProduct(
                entities.find(_.productId == product1.id).get,
                inventoryCountProduct1,
                product1,
              )
              assertResponseInventoryCountProduct(
                entities.find(_.productId == product2.id).get,
                inventoryCountProduct2,
                product2,
              )
            }
          }
        }

        "with expand[]=options" should {
          "return the inventory count" in new InventoryCountsFSpecContext {
            val simpleProduct = Factory.simpleProduct(merchant).create

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

            val inventoryCount = Factory.inventoryCount(london, user).create
            val inventoryCountProduct1 = Factory.inventoryCountProduct(inventoryCount, simpleProduct).create
            val inventoryCountProduct2 = Factory.inventoryCountProduct(inventoryCount, variantProduct).create

            Get(s"/v1/inventory_counts.list_products?inventory_count_id=${inventoryCount.id}&expand[]=options")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val entities = responseAs[ApiResponse[Seq[InventoryCountProduct]]].data
              assertResponseInventoryCountProduct(
                entities.find(_.productId == simpleProduct.id).get,
                inventoryCountProduct1,
                simpleProduct,
              )
              assertResponseInventoryCountProduct(
                entities.find(_.productId == variantProduct.id).get,
                inventoryCountProduct2,
                variantProduct,
                Seq(variantOptionWithType),
              )
            }
          }
        }
      }

      "if the inventory count does not belong to the merchant" should {
        "return empty list" in new InventoryCountsFSpecContext {
          val competitor = Factory.merchant.create
          val locationCompetitor = Factory.location(competitor).create
          val userCompetitor = Factory.user(competitor).create
          val inventoryCountCompetitor =
            Factory.inventoryCount(locationCompetitor, userCompetitor).create

          Get(s"/v1/inventory_counts.list_products?inventory_count_id=${inventoryCountCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[InventoryCountProduct]]].data
            entities must beEmpty
          }
        }
      }

      "if the inventory count does not exist" should {
        "return empty list" in new InventoryCountsFSpecContext {
          Get(s"/v1/inventory_counts.list_products?inventory_count_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[ApiResponse[Seq[InventoryCountProduct]]].data
            entities must beEmpty
          }
        }
      }
    }
  }
}
