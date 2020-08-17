package io.paytouch.core.resources.inventories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductsListInventoryFSpec extends GenericListInventoryFSpec {
  abstract class ProductsListInventoryFSpecContext extends GenericListInventoryFSpecContext

  "GET /v1/products.list_inventory" in {
    "if request has valid token" in {
      "with no filters" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          Get(s"/v1/products.list_inventory").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(templateProduct.id, simpleProduct.id))

            val templateInventory = inventory.find(_.id == templateProduct.id).get
            assertInventoryMatchesExpectedValues(
              templateInventory,
              quantity = 7.0,
              stockValue = 14.84.$$$,
            )

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )
          }
        }
      }

      "with category_id filter" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          Get(s"/v1/products.list_inventory?category_id=${tShirts.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id))

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )
          }
        }
      }

      "with category_id[] filter" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          val books = Factory.systemCategory(defaultMenuCatalog).create
          val simpleProductBook = Factory.simpleProduct(merchant, categories = Seq(books)).create

          Get(s"/v1/products.list_inventory?category_id[]=${tShirts.id},${books.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id, simpleProductBook.id))
          }
        }
      }

      "with location_id filter" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          val simpleProductRome =
            Factory
              .productLocation(
                simpleProduct,
                rome,
                costAmount = Some(3.15),
              )
              .create

          Factory
            .stock(
              simpleProductRome,
              quantity = Some(4),
            )
            .create

          val orderPaidRome =
            Factory
              .order(
                merchant,
                location = Some(rome),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create

          Factory
            .orderItem(
              orderPaidRome,
              product = Some(simpleProduct),
              quantity = Some(5),
              priceAmount = Some(BigDecimal(9)),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create

          def assertSimpleProductRome(locationIds: String): Unit =
            Get(s"/v1/products.list_inventory?$locationIds")
              .addHeader(authorizationHeader) ~> routes ~> check {

              assertStatusOK()

              val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data

              inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id))

              assertInventoryMatchesExpectedValues(
                inventory.find(_.id == simpleProduct.id).get,
                quantity = 4.0,
                stockValue = (4 * 3.15).$$$,
              )
            }

          val londonId = london.id
          val romeId = rome.id

          assertSimpleProductRome(s"location_id=$romeId")
          assertSimpleProductRome(s"location_id[]=$romeId")
          assertSimpleProductRome(s"location_id=$romeId&location_id[]=$romeId")
          assertSimpleProductRome(s"location_id=$romeId&location_id[]=$romeId,${UUID.randomUUID()}")
        }
      }

      "with low filter" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          val simpleProductRome = Factory.productLocation(simpleProduct, rome).create
          Factory.stock(simpleProductRome, quantity = Some(4)).create

          val orderPaidRome =
            Factory.order(merchant, location = Some(rome), paymentStatus = Some(PaymentStatus.Paid)).create
          Factory
            .orderItem(
              orderPaidRome,
              product = Some(simpleProduct),
              quantity = Some(5),
              priceAmount = Some(BigDecimal(9)),
              paymentStatus = Some(PaymentStatus.Paid),
            )
            .create

          Get(s"/v1/products.list_inventory?low=true").addHeader(authorizationHeader) ~> routes ~> check {

            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id))
          }
        }
      }

      "with q filter" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          Get(s"/v1/products.list_inventory?q=123aniel321").addHeader(authorizationHeader) ~> routes ~> check {

            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id))

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )
          }
        }
      }

      "with supplier_id filter" in {
        "returns a paginated list of a product and their inventories" in new ProductsListInventoryFSpecContext {
          Get(s"/v1/products.list_inventory?supplier_id=${supplier.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id))

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )
          }
        }
      }
    }
  }
}
