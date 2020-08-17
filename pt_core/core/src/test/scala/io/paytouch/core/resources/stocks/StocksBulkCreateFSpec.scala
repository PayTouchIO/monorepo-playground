package io.paytouch.core.resources.stocks

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class StocksBulkCreateFSpec extends StocksFSpec {

  abstract class StocksBulkCreateFSpecContext extends StockResourceFSpecContext

  "POST /v1/stocks.bulk_create" in {
    "if request has valid token" in {
      "if all values are valid" should {
        "create stocks and return 201" in new StocksBulkCreateFSpecContext {
          val variants = Seq(variant1, variant2)
          val creations =
            variants.zip(random[StockCreation](2)).map {
              case (variant, creation) =>
                creation.copy(locationId = london.id, productId = variant.id, sellOutOfStock = None)
            }

          Post("/v1/stocks.bulk_create", creations).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val stocks = responseAs[ApiResponse[Seq[Stock]]].data

            assertBulkResponse(stocks)
            assertBulkCreation(creations, stocks)
          }
        }
      }
      "if a product is not storable" should {
        "reject the request and return 400" in new StocksBulkCreateFSpecContext {
          val products = Seq(template, variant2)
          val creations =
            products.zip(random[StockCreation](2)).map {
              case (product, creation) =>
                creation.copy(locationId = london.id, productId = product.id, sellOutOfStock = None)
            }
          Post("/v1/stocks.bulk_create", creations).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
      "if a product-location ids are duplicated" should {
        "reject the request and return 400" in new StocksBulkCreateFSpecContext {
          val creations = {
            val stockCreations = random[StockCreation](2)
            val stockCreation1 =
              stockCreations(0).copy(locationId = london.id, productId = template.id, sellOutOfStock = Some(true))
            val stockCreation2 =
              stockCreations(1).copy(locationId = london.id, productId = variant2.id, sellOutOfStock = Some(true))
            Seq(stockCreation1, stockCreation2)
          }

          Post(s"/v1/stocks.bulk_create", creations).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new StocksBulkCreateFSpecContext {
        Post("/v1/stocks.bulk_create", Seq.empty).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
