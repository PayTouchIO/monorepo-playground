package io.paytouch.core.resources.brands

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class BrandsCreateFSpec extends BrandsFSpec {

  abstract class BrandsCreateFSpecContext extends BrandResourceFSpecContext

  "POST /v1/brands.create?brand_id=$" in {
    "if request has valid token" in {

      "create brand and return 201" in new BrandsCreateFSpecContext {
        val newBrandId = UUID.randomUUID
        val creation = random[BrandCreation]

        Post(s"/v1/brands.create?brand_id=$newBrandId", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val brand = responseAs[ApiResponse[Brand]].data
          assertCreation(brand.id, creation)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new BrandsCreateFSpecContext {
        val newBrandId = UUID.randomUUID
        val creation = random[BrandCreation]
        Post(s"/v1/brands.create?brand_id=$newBrandId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
