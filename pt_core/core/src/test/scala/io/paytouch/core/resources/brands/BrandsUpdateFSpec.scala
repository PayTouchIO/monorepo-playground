package io.paytouch.core.resources.brands

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class BrandsUpdateFSpec extends BrandsFSpec {

  abstract class BrandsUpdateFSpecContext extends BrandResourceFSpecContext

  "POST /v1/brands.update?brand_id=$" in {
    "if request has valid token" in {
      "if brand belong to same merchant" should {
        "update brand and return 200" in new BrandsUpdateFSpecContext {
          val brand = Factory.brand(merchant).create
          val update = random[BrandUpdate]

          Post(s"/v1/brands.update?brand_id=${brand.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(brand.id, update)
          }
        }
      }
      "if brand doesn't belong to current user's merchant" in {
        "not update brand and return 404" in new BrandsUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorBrand = Factory.brand(competitor).create

          val update = random[BrandUpdate]

          Post(s"/v1/brands.update?brand_id=${competitorBrand.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedBrand = brandDao.findById(competitorBrand.id).await.get
            updatedBrand ==== competitorBrand
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new BrandsUpdateFSpecContext {
        val brandId = UUID.randomUUID
        val update = random[BrandUpdate]
        Post(s"/v1/brands.update?brand_id=$brandId", update).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
