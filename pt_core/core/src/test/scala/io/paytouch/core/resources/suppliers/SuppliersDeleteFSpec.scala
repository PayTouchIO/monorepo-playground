package io.paytouch.core.resources.suppliers

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SuppliersDeleteFSpec extends SuppliersFSpec {

  abstract class SupplierDeleteResourceFSpecContext extends SupplierResourceFSpecContext {
    def assertSupplierExists(id: UUID) = supplierDao.findById(id).await should beSome

    def assertSupplierIsMarkedAsDeleted(id: UUID) = supplierDao.findDeletedById(id).await should beSome

    def assertSupplierDoesntExist(id: UUID) = {
      supplierDao.findDeletedById(id).await should beNone
      supplierDao.findById(id).await should beNone
    }
  }

  "POST /v1/suppliers.delete" in {

    "if request has valid token" in {
      "if supplier doesn't exist" should {
        "do nothing and return 204" in new SupplierDeleteResourceFSpecContext {
          val nonExistingSupplierId = UUID.randomUUID

          Post(s"/v1/suppliers.delete", Ids(ids = Seq(nonExistingSupplierId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertSupplierDoesntExist(nonExistingSupplierId)
          }
        }
      }

      "if supplier belongs to the merchant" should {
        "delete the supplier and return 204" in new SupplierDeleteResourceFSpecContext {
          val supplier = Factory.supplier(merchant).create

          Post(s"/v1/suppliers.delete", Ids(ids = Seq(supplier.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertSupplierIsMarkedAsDeleted(supplier.id)
          }
        }
      }

      "if supplier belongs to a different merchant" should {
        "do not delete the supplier and return 204" in new SupplierDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorSupplier = Factory.supplier(competitor).create

          Post(s"/v1/suppliers.delete", Ids(ids = Seq(competitorSupplier.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertSupplierExists(competitorSupplier.id)
          }
        }
      }
    }
  }
}
