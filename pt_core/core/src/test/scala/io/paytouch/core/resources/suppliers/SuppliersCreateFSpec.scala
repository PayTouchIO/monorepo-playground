package io.paytouch.core.resources.suppliers

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities.{ ApiResponse, ItemLocationUpdate, Supplier, SupplierCreation }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class SuppliersCreateFSpec extends SuppliersFSpec {

  abstract class SupplierCreateFSpecContext extends SupplierResourceFSpecContext

  "POST /v1/suppliers.create?supplier_id=$" in {
    "if request has valid token" in {
      "create supplier nd return 201" in new SupplierCreateFSpecContext {
        val newSupplierId = UUID.randomUUID

        val simpleProduct = Factory.simpleProduct(merchant).create
        val templateProduct = Factory.templateProduct(merchant).create
        val variantProduct = Factory.variantProduct(merchant, templateProduct).create

        val locationOverrides = Map(london.id -> Some(ItemLocationUpdate(Some(true))))
        val supplierCreation = random[SupplierCreation].copy(
          locationOverrides = locationOverrides,
          productIds = Seq(simpleProduct.id, templateProduct.id),
          email = Some(randomEmail),
        )

        Post(s"/v1/suppliers.create?supplier_id=$newSupplierId", supplierCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val supplier = responseAs[ApiResponse[Supplier]].data
          assertCreation(supplierCreation, supplier.id)
        }
      }

      "reject request if any location id does not exist of does not belong to merchant" in new SupplierCreateFSpecContext {
        val competitor = Factory.merchant.create
        val competitorLocation = Factory.location(competitor).create

        val competitorLocationOverrides = Map(competitorLocation.id -> None)
        val newSupplierId = UUID.randomUUID
        val supplierCreation =
          random[SupplierCreation].copy(
            locationOverrides = competitorLocationOverrides,
            productIds = Seq.empty,
            email = Some(randomEmail),
          )

        Post(s"/v1/suppliers.create?supplier_id=$newSupplierId", supplierCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)

          supplierDao.findById(newSupplierId).await ==== None
          itemLocationDao.findByItemId(newSupplierId).await ==== Seq.empty
          supplierProductDao.findBySupplierId(newSupplierId).await ==== Seq.empty
        }
      }

      "reject request if any product id is not a main product id" in new SupplierCreateFSpecContext {
        val competitor = Factory.merchant.create

        val templateProduct = Factory.templateProduct(merchant).create
        val variantProduct = Factory.variantProduct(merchant, templateProduct).create

        val locationOverrides = Map(london.id -> None)
        val newSupplierId = UUID.randomUUID
        val supplierCreation =
          random[SupplierCreation].copy(productIds = Seq(variantProduct.id), email = Some(randomEmail))

        Post(s"/v1/suppliers.create?supplier_id=$newSupplierId", supplierCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)

          supplierDao.findById(newSupplierId).await ==== None
          itemLocationDao.findByItemId(newSupplierId).await ==== Seq.empty
          supplierProductDao.findBySupplierId(newSupplierId).await ==== Seq.empty
        }
      }

      "if supplier email is invalid" should {
        "return 400" in new SupplierCreateFSpecContext {
          val newSupplierId = UUID.randomUUID
          val supplierCreation = random[SupplierCreation].copy(email = Some("yadda"), productIds = Seq.empty)

          Post(s"/v1/suppliers.create?supplier_id=$newSupplierId", supplierCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new SupplierCreateFSpecContext {
        val newSupplierId = UUID.randomUUID
        val supplierCreation = random[SupplierCreation]
        Post(s"/v1/suppliers.create?supplier_id=$newSupplierId", supplierCreation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
