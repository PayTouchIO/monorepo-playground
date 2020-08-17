package io.paytouch.core.resources.suppliers

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class SuppliersListFSpec extends SuppliersFSpec {

  abstract class SupplierListFSpecContext extends SupplierResourceFSpecContext

  "GET /v1/suppliers.list" in {
    "if request has valid token" in {
      "with no parameters" should {
        "return a paginated list of suppliers" in new SupplierListFSpecContext {
          val supplier1 = Factory.supplier(merchant, name = Some("Alphabetically")).create
          val supplier2 = Factory.supplier(merchant, name = Some("Ordered")).create
          val supplier3 = Factory.supplier(merchant, name = Some("Zar")).create
          val supplier4 = Factory.supplier(merchant, deletedAt = Some(UtcTime.now)).create

          Get("/v1/suppliers.list?per_page=2&page=1").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id) ==== Seq(supplier1.id, supplier2.id)
            suppliers.pagination.totalCount ==== 3

            assertResponse(suppliers.data.find(_.id == supplier1.id).get, supplier1)
            assertResponse(suppliers.data.find(_.id == supplier2.id).get, supplier2)
          }
        }
      }

      "with location_id filter" should {
        "return a paginated list of suppliers matching the filters" in new SupplierListFSpecContext {
          val supplierUK1 = Factory.supplier(merchant, locations = Seq(london)).create
          val supplierUK2 = Factory.supplier(merchant, locations = Seq(london)).create
          val supplierIta = Factory.supplier(merchant, locations = Seq(rome)).create

          Get(s"/v1/suppliers.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]

            suppliers.data.map(_.id) ==== Seq(supplierIta.id)
            assertResponse(suppliers.data.head, supplierIta)
          }
        }
      }

      "with category_id filter" should {
        "return a paginated list of suppliers matching the filters" in new SupplierListFSpecContext {
          val category = Factory.systemCategory(defaultMenuCatalog).create
          val anotherCategory = Factory.systemCategory(defaultMenuCatalog).create
          val productInCategory = Factory.simpleProduct(merchant, categories = Seq(category)).create
          val productInAnotherCategory = Factory.simpleProduct(merchant, categories = Seq(anotherCategory)).create

          val supplierWithNoProducts = Factory.supplier(merchant).create
          val supplierWithProductsInCategory = Factory.supplier(merchant).create
          val supplierWithProductsInAnotherCategory = Factory.supplier(merchant).create

          Factory.supplierProduct(supplierWithProductsInCategory, productInCategory).create
          Factory.supplierProduct(supplierWithProductsInAnotherCategory, productInAnotherCategory).create

          Get(s"/v1/suppliers.list?category_id=${category.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]

            suppliers.data.map(_.id) ==== Seq(supplierWithProductsInCategory.id)
            assertResponse(suppliers.data.head, supplierWithProductsInCategory)
          }
        }
      }

      "with category_id[] filter" should {
        "return a paginated list of suppliers matching the filters" in new SupplierListFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create
          val category3 = Factory.systemCategory(defaultMenuCatalog).create
          val productInCategory1 = Factory.simpleProduct(merchant, categories = Seq(category1)).create
          val productInCategory2 = Factory.simpleProduct(merchant, categories = Seq(category2)).create
          val productInCategory3 = Factory.simpleProduct(merchant, categories = Seq(category3)).create

          val supplierWithNoProducts = Factory.supplier(merchant).create
          val supplierWithProductsInCategory1 = Factory.supplier(merchant).create
          val supplierWithProductsInCategory2 = Factory.supplier(merchant).create
          val supplierWithProductsInCategory3 = Factory.supplier(merchant).create

          Factory.supplierProduct(supplierWithProductsInCategory1, productInCategory1).create
          Factory.supplierProduct(supplierWithProductsInCategory2, productInCategory2).create
          Factory.supplierProduct(supplierWithProductsInCategory3, productInCategory3).create

          Get(s"/v1/suppliers.list?category_id[]=${category1.id},${category2.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]

            suppliers.data.map(_.id) should containTheSameElementsAs(
              Seq(supplierWithProductsInCategory1.id, supplierWithProductsInCategory2.id),
            )
            assertResponse(
              suppliers.data.find(_.id == supplierWithProductsInCategory1.id).get,
              supplierWithProductsInCategory1,
            )
            assertResponse(
              suppliers.data.find(_.id == supplierWithProductsInCategory2.id).get,
              supplierWithProductsInCategory2,
            )
          }
        }
      }

      "with q filter" should {
        "return a paginated list of suppliers with name matching query" in new SupplierListFSpecContext {
          val supplier1 = Factory.supplier(merchant, name = Some("UK Supplier1")).create
          val supplier2 = Factory.supplier(merchant, name = Some("UK Supplier2")).create
          val supplier3 = Factory.supplier(merchant, name = Some("IT Supplier")).create

          Get(s"/v1/suppliers.list?q=Supplier2").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]

            suppliers.data.map(_.id) ==== Seq(supplier2.id)
            assertResponse(suppliers.data.head, supplier2)
          }
        }
      }

      "with expand[]=products_count" should {
        "return a paginated list of suppliers with products count expanded" in new SupplierListFSpecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create
          val product3 = Factory.simpleProduct(merchant).create

          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          Factory.supplierProduct(supplier1, product1).create
          Factory.supplierProduct(supplier1, product2).create
          Factory.supplierProduct(supplier2, product3).create

          Get(s"/v1/suppliers.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id).toSet ==== Set(supplier1.id, supplier2.id)
            suppliers.pagination.totalCount ==== 2

            assertResponse(suppliers.data.find(_.id == supplier1.id).get, supplier1, productsCount = Some(2))
            assertResponse(suppliers.data.find(_.id == supplier2.id).get, supplier2, productsCount = Some(1))
          }
        }
      }

      "with expand[]=products_count and deleted product" should {
        "return a paginated list of suppliers with products count expanded" in new SupplierListFSpecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant, deletedAt = Some(UtcTime.now)).create

          val supplier1 = Factory.supplier(merchant).create

          Factory.supplierProduct(supplier1, product1).create
          Factory.supplierProduct(supplier1, product2).create

          Get(s"/v1/suppliers.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id).toSet ==== Set(supplier1.id)
            suppliers.pagination.totalCount ==== 1

            assertResponse(suppliers.data.find(_.id == supplier1.id).get, supplier1, productsCount = Some(1))
          }
        }
      }

      "with expand[]=products_count and location_id/category_id filter" should {
        "return a paginated list of suppliers with products count expanded" in new SupplierListFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create

          val product1 = Factory.simpleProduct(merchant, categories = Seq(category1)).create
          val product2 = Factory.simpleProduct(merchant, categories = Seq(category2)).create
          val product3 = Factory.simpleProduct(merchant, categories = Seq(category2)).create

          val supplier1 = Factory.supplier(merchant, locations = Seq(rome)).create
          val supplier2 = Factory.supplier(merchant, locations = Seq(london)).create

          Factory.supplierProduct(supplier1, product1).create
          Factory.supplierProduct(supplier1, product2).create
          Factory.supplierProduct(supplier2, product3).create

          Get(s"/v1/suppliers.list?expand[]=products_count&location_id=${rome.id}&category_id=${category1.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id).toSet ==== Set(supplier1.id)
            suppliers.pagination.totalCount ==== 1

            assertResponse(suppliers.data.find(_.id == supplier1.id).get, supplier1, productsCount = Some(1))
          }
        }
      }

      "with expand[]=stock_values" should {
        "return a paginated list of suppliers with product quantities expanded" in new SupplierListFSpecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create
          val product3 = Factory.simpleProduct(merchant).create

          val product1Rome = Factory.productLocation(product1, rome, costAmount = Some(2.0)).create
          val product1Stock = Factory.stock(product1Rome, quantity = Some(3)).create
          val product2Rome = Factory.productLocation(product2, rome, costAmount = Some(4.0)).create
          val product2Stock = Factory.stock(product2Rome, quantity = Some(6)).create
          val product3Rome = Factory.productLocation(product3, rome, costAmount = Some(8.0)).create
          val product3Stock = Factory.stock(product3Rome, quantity = Some(9)).create

          val supplier1 = Factory.supplier(merchant).create
          val supplier2 = Factory.supplier(merchant).create

          Factory.supplierProduct(supplier1, product1).create
          Factory.supplierProduct(supplier1, product2).create
          Factory.supplierProduct(supplier2, product3).create

          Get(s"/v1/suppliers.list?expand[]=stock_values").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id).toSet ==== Set(supplier1.id, supplier2.id)
            suppliers.pagination.totalCount ==== 2

            assertResponse(suppliers.data.find(_.id == supplier1.id).get, supplier1, stockValue = Some(30.$$$))
            assertResponse(suppliers.data.find(_.id == supplier2.id).get, supplier2, stockValue = Some(72.$$$))
          }
        }
      }

      "with expand[]=stock_values and location_id/category_id filter" should {
        "return a paginated list of suppliers with product quantities expanded" in new SupplierListFSpecContext {
          val category1 = Factory.systemCategory(defaultMenuCatalog).create
          val category2 = Factory.systemCategory(defaultMenuCatalog).create

          val product1 = Factory.simpleProduct(merchant, categories = Seq(category1)).create
          val product2 = Factory.simpleProduct(merchant, categories = Seq(category2)).create
          val product3 = Factory.simpleProduct(merchant, categories = Seq(category2)).create

          val product1Rome =
            Factory.productLocation(product1, rome, costAmount = Some(2.0)).create
          val product1Stock = Factory.stock(product1Rome, quantity = Some(3)).create
          val product2London = Factory
            .productLocation(product2, london, costAmount = Some(4.0))
            .create
          val product2Stock = Factory.stock(product2London, quantity = Some(6)).create

          val product3Rome =
            Factory.productLocation(product3, rome, costAmount = Some(8.0)).create
          val product3Stock = Factory.stock(product3Rome, quantity = Some(9)).create

          val supplier1 = Factory.supplier(merchant, locations = Seq(rome, london)).create
          val supplier2 = Factory.supplier(merchant, locations = Seq(london)).create

          Factory.supplierProduct(supplier1, product1).create
          Factory.supplierProduct(supplier1, product2).create
          Factory.supplierProduct(supplier2, product3).create

          Get(s"/v1/suppliers.list?expand[]=stock_values&location_id=${rome.id}&category_id=${category1.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id).toSet ==== Set(supplier1.id)
            suppliers.pagination.totalCount ==== 1

            assertResponse(suppliers.data.find(_.id == supplier1.id).get, supplier1, stockValue = Some(6.$$$))
          }
        }
      }

      "with expand[]=locations" should {
        "return a paginated list of suppliers with location expanded" in new SupplierListFSpecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create
          val product3 = Factory.simpleProduct(merchant).create

          val supplier1 = Factory.supplier(merchant).create
          Factory.supplierProduct(supplier1, product1).create
          Factory.supplierProduct(supplier1, product2).create
          Factory.supplierLocation(supplier1, rome, active = Some(true)).create

          val supplier2 = Factory.supplier(merchant).create
          Factory.supplierProduct(supplier2, product3).create
          Factory.supplierLocation(supplier2, rome, active = Some(true)).create
          Factory.supplierLocation(supplier2, london, active = Some(false)).create

          Get(s"/v1/suppliers.list?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val suppliers = responseAs[PaginatedApiResponse[Seq[Supplier]]]
            suppliers.data.map(_.id).toSet ==== Set(supplier1.id, supplier2.id)
            suppliers.pagination.totalCount ==== 2

            val locationOverrides1 = Map(rome.id -> ItemLocation(true))
            val locationOverrides2 = Map(rome.id -> ItemLocation(true), london.id -> ItemLocation(false))

            assertResponse(
              suppliers.data.find(_.id == supplier1.id).get,
              supplier1,
              locationOverrides = Some(locationOverrides1),
            )
            assertResponse(
              suppliers.data.find(_.id == supplier2.id).get,
              supplier2,
              locationOverrides = Some(locationOverrides2),
            )
          }
        }
      }
    }
  }

}
