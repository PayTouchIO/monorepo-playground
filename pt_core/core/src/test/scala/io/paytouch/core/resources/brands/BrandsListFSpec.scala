package io.paytouch.core.resources.brands

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class BrandsListFSpec extends BrandsFSpec {

  abstract class BrandsListFSpecContext extends BrandResourceFSpecContext

  "GET /v1/brands.list" in {
    "if request has valid token" should {
      "return a paginated list of brands" in new BrandResourceFSpecContext {
        val brand1 = Factory.brand(merchant, name = Some("Alphabetically")).create
        val brand2 = Factory.brand(merchant, name = Some("Ordered")).create

        Get("/v1/brands.list").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val brands = responseAs[PaginatedApiResponse[Seq[Brand]]].data
          brands.map(_.id) ==== Seq(brand1.id, brand2.id)

          assertResponse(brands.find(_.id == brand1.id).get, brand1)
          assertResponse(brands.find(_.id == brand2.id).get, brand2)
        }
      }
    }
  }

}
