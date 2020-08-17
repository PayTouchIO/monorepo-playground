package io.paytouch.core.resources.suppliers

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SuppliersGetFSpec extends SuppliersFSpec {

  abstract class SuppliersGetFSpecContext extends SupplierResourceFSpecContext

  "GET /v1/suppliers.get?supplier_id=$" in {
    "if request has valid token" in {

      "if the supplier exists" should {

        "with no parameters" should {
          "return the supplier" in new SuppliersGetFSpecContext {
            val supplierRecord = Factory.supplier(merchant).create

            Get(s"/v1/suppliers.get?supplier_id=${supplierRecord.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val supplierEntity = responseAs[ApiResponse[Supplier]].data
              assertResponse(supplierEntity, supplierRecord)
            }
          }
        }

        "with expand=products_count" should {
          "return the supplier" in new SuppliersGetFSpecContext {
            val supplierRecord = Factory.supplier(merchant).create

            val product1 = Factory.simpleProduct(merchant).create
            val product2 = Factory.simpleProduct(merchant).create
            val product3 = Factory.simpleProduct(merchant).create

            Factory.supplierProduct(supplierRecord, product1).create
            Factory.supplierProduct(supplierRecord, product2).create

            Get(s"/v1/suppliers.get?supplier_id=${supplierRecord.id}&expand[]=products_count")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val supplierEntity = responseAs[ApiResponse[Supplier]].data
              assertResponse(supplierEntity, supplierRecord, productsCount = Some(2))
            }
          }
        }

        "with expand=stock_values" should {
          "return the supplier" in new SuppliersGetFSpecContext {
            val supplierRecord = Factory.supplier(merchant).create

            val product1 = Factory.simpleProduct(merchant).create
            val product2 = Factory.simpleProduct(merchant).create
            val product3 = Factory.simpleProduct(merchant).create

            val product1Rome = Factory.productLocation(product1, rome, costAmount = Some(2.0)).create
            val product1Stock = Factory.stock(product1Rome, quantity = Some(3)).create
            val product2Rome = Factory.productLocation(product2, rome, costAmount = Some(4.0)).create
            val product2Stock = Factory.stock(product2Rome, quantity = Some(6)).create
            val product3Rome = Factory.productLocation(product3, rome, costAmount = Some(8.0)).create
            val product3Stock = Factory.stock(product3Rome, quantity = Some(9)).create

            Factory.supplierProduct(supplierRecord, product1).create
            Factory.supplierProduct(supplierRecord, product2).create

            Get(s"/v1/suppliers.get?supplier_id=${supplierRecord.id}&expand[]=stock_values")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val supplierEntity = responseAs[ApiResponse[Supplier]].data
              assertResponse(supplierEntity, supplierRecord, stockValue = Some(30.$$$))
            }
          }
        }

        "with expand=locations" should {
          "return the supplier" in new SuppliersGetFSpecContext {
            val supplierRecord = Factory.supplier(merchant).create
            Factory.supplierLocation(supplierRecord, rome, active = Some(true)).create

            Get(s"/v1/suppliers.get?supplier_id=${supplierRecord.id}&expand[]=locations")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val supplierEntity = responseAs[ApiResponse[Supplier]].data

              val locationOverrides = Map(rome.id -> ItemLocation(true))
              assertResponse(supplierEntity, supplierRecord, locationOverrides = Some(locationOverrides))
            }
          }
        }
      }

      "if the supplier does not belong to the merchant" should {
        "return 404" in new SuppliersGetFSpecContext {
          val competitor = Factory.merchant.create
          val supplierCompetitor = Factory.supplier(competitor).create

          Get(s"/v1/suppliers.get?supplier_id=${supplierCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the supplier does not exist" should {
        "return 404" in new SuppliersGetFSpecContext {
          Get(s"/v1/suppliers.get?supplier_id=${UUID.randomUUID}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
