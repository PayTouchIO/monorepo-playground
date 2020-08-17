package io.paytouch.core.resources.suppliers

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SuppliersUpdateFSpec extends SuppliersFSpec {

  abstract class SupplierUpdateFSpecContext extends SupplierResourceFSpecContext

  "POST /v1/suppliers.update?supplier_id=$" in {

    "if request has valid token" in {
      "if supplier belong to same merchant" should {
        "update supplier and return 200" in new SupplierUpdateFSpecContext {
          val supplier = Factory.supplier(merchant).create

          val simpleProduct = Factory.simpleProduct(merchant).create
          val templateProduct = Factory.templateProduct(merchant).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct).create

          val locationOverrides = Map(london.id -> Some(ItemLocationUpdate(Some(true))))
          val supplierUpdate =
            random[SupplierUpdate].copy(
              locationOverrides = locationOverrides,
              productIds = Some(Seq(simpleProduct.id, templateProduct.id)),
              email = Some(randomEmail),
            )

          Post(s"/v1/suppliers.update?supplier_id=${supplier.id}", supplierUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assertUpdate(supplierUpdate, supplier.id)
          }
        }

        "reject request if any location id does not exist of does not belong to merchant" in new SupplierUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          val competitorLocationOverrides = Map(competitorLocation.id -> None)
          val supplier = Factory.supplier(merchant).create
          val supplierUpdate =
            random[SupplierUpdate].copy(locationOverrides = competitorLocationOverrides, email = Some(randomEmail))

          Post(s"/v1/suppliers.update?supplier_id=${supplier.id}", supplierUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }

        "reject request if any product id is not a main product id" in new SupplierUpdateFSpecContext {
          val competitor = Factory.merchant.create

          val templateProduct = Factory.templateProduct(merchant).create
          val variantProduct = Factory.variantProduct(merchant, templateProduct).create

          val locationOverrides = Map(london.id -> None)
          val supplier = Factory.supplier(merchant).create
          val supplierUpdate =
            random[SupplierUpdate].copy(productIds = Some(Seq(variantProduct.id)), email = Some(randomEmail))

          Post(s"/v1/suppliers.update?supplier_id=${supplier.id}", supplierUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if supplier doesn't belong to current user's merchant" in {
        "not update supplier and return 404" in new SupplierUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorSupplier = Factory.supplier(competitor).create

          val supplierUpdate = random[SupplierUpdate]

          Post(s"/v1/suppliers.update?supplier_id=${competitorSupplier.id}", supplierUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if supplier email is invalid" should {
        "return 400" in new SupplierUpdateFSpecContext {
          val supplier = Factory.supplier(merchant).create
          val supplierUpdate = random[SupplierUpdate].copy(email = Some("yadda"), productIds = None)

          Post(s"/v1/suppliers.update?supplier_id=${supplier.id}", supplierUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new SupplierUpdateFSpecContext {
        val supplierId = UUID.randomUUID
        val supplierUpdate = random[SupplierUpdate]
        Post(s"/v1/suppliers.update?supplier_id=$supplierId", supplierUpdate)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
