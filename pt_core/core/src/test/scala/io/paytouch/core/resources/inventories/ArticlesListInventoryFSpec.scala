package io.paytouch.core.resources.inventories

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.enums.{ ArticleTypeAlias, PaymentStatus }
import io.paytouch.core.entities._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ArticlesListInventoryFSpec extends GenericListInventoryFSpec {
  abstract class ArticlesListInventoryFSpecContext extends GenericListInventoryFSpecContext

  "GET /v1/articles.list_inventory" in {
    "if request has valid token" in {
      "with no filters" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(
              Seq(templateProduct.id, simpleProduct.id, simplePart.id),
            )

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

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with scope filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?scope=part").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simplePart.id))

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with type filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?type=${ArticleTypeAlias.Storable.entryName}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(variantProduct.id, simpleProduct.id, simplePart.id))

            val variantInventory = inventory.find(_.id == variantProduct.id).get
            assertInventoryMatchesExpectedValues(
              variantInventory,
              quantity = 7.0,
              stockValue = 14.84.$$$,
            )

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with type[] filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?type[]=variant,simple")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(variantProduct.id, simpleProduct.id, simplePart.id))

            val variantInventory = inventory.find(_.id == variantProduct.id).get
            assertInventoryMatchesExpectedValues(
              variantInventory,
              quantity = 7.0,
              stockValue = 14.84.$$$,
            )

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with is_combo filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?is_combo=false")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(
              Seq(templateProduct.id, simpleProduct.id, simplePart.id),
            )

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

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with category_id filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?category_id=${tShirts.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id, simplePart.id))

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with category_id[] filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          val books = Factory.systemCategory(defaultMenuCatalog).create
          val simpleProductBook = Factory.simpleProduct(merchant, categories = Seq(books)).create

          Get(s"/v1/articles.list_inventory?category_id[]=${tShirts.id},${books.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(
              Seq(simpleProduct.id, simpleProductBook.id, simplePart.id),
            )
          }
        }
      }

      "with location_id filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
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
            Get(s"/v1/articles.list_inventory?$locationIds")
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
        "with simple products low stock in any location" in {
          "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
            val simpleProductRome = Factory.productLocation(simpleProduct, rome).create
            Factory.stock(simpleProductRome, quantity = Some(20), minimumOnHand = Some(10)).create // not low stock

            Get(s"/v1/articles.list_inventory?low=true").addHeader(authorizationHeader) ~> routes ~> check {

              assertStatusOK()
              val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
              inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id, simplePart.id))
            }
          }
        }

        "with simple products low stock in a location and filtered for the other" in {
          "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
            val simpleProductRome = Factory.productLocation(simpleProduct, rome).create
            Factory.stock(simpleProductRome, quantity = Some(20), minimumOnHand = Some(10)).create // not low stock

            val simplePartRome = Factory.productLocation(simplePart, rome).create
            Factory.stock(simplePartRome, quantity = Some(5), minimumOnHand = Some(20)).create

            Get(s"/v1/articles.list_inventory?low=true&location_id=${rome.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {

              assertStatusOK()
              val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
              inventory.map(_.id) should containTheSameElementsAs(Seq(simplePart.id))
            }
          }
        }

        "with variant products low stock in any location" in {
          "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
            val variantProductRome = Factory.productLocation(variantProduct, rome).create
            Factory.stock(variantProductRome, quantity = Some(5), minimumOnHand = Some(20)).create

            Get(s"/v1/articles.list_inventory?low=true").addHeader(authorizationHeader) ~> routes ~> check {

              assertStatusOK()
              val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
              inventory.map(_.id) should containTheSameElementsAs(
                Seq(simpleProduct.id, simplePart.id, templateProduct.id),
              )
            }
          }
        }

      }

      "with q filter" in {
        "returns a paginated list of articles and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?q=123aniel321").addHeader(authorizationHeader) ~> routes ~> check {

            assertStatusOK()
            val inventory = responseAs[PaginatedApiResponse[Seq[Inventory]]].data
            inventory.map(_.id) should containTheSameElementsAs(Seq(simpleProduct.id, simplePart.id))

            val simpleProductInventory = inventory.find(_.id == simpleProduct.id).get
            assertInventoryMatchesExpectedValues(
              simpleProductInventory,
              quantity = 3.0,
              stockValue = (3 * 3.15).$$$,
            )

            val simplePartInventory = inventory.find(_.id == simplePart.id).get
            assertInventoryMatchesExpectedValues(simplePartInventory, quantity = 5, stockValue = (5 * 4.15).$$$)
          }
        }
      }

      "with supplier_id filter" in {
        "returns a paginated list of a product and their inventories" in new ArticlesListInventoryFSpecContext {
          Get(s"/v1/articles.list_inventory?supplier_id=${supplier.id}")
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
