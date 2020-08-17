package io.paytouch.core.resources.stocks

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class StocksBulkUpdateFSpec extends StocksFSpec {

  abstract class StocksBulkUpdateFSpecContext extends StockResourceFSpecContext

  "POST /v1/stocks.bulk_update" in {
    "if request has valid token" in {
      "if all values are valid" should {
        "update stocks and return 200" in new StocksBulkUpdateFSpecContext {
          val variant1Stock = Factory.stock(variant1London, quantity = Some(3)).create
          val variant2Stock = Factory.stock(variant2London, quantity = Some(4)).create
          val updates = {
            val stockUpdates = random[StockUpdate](2)
            val stockUpdate1 =
              stockUpdates(0).copy(locationId = london.id, productId = variant1.id, sellOutOfStock = Some(true))
            val stockUpdate2 =
              stockUpdates(1).copy(locationId = london.id, productId = variant2.id, sellOutOfStock = Some(true))
            Seq(stockUpdate1, stockUpdate2)
          }

          Post(s"/v1/stocks.bulk_update", updates).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val stocks = responseAs[ApiResponse[Seq[Stock]]].data
            assertBulkUpdate(updates)
            assertBulkResponse(stocks)
          }
        }
      }
      "if a product is not storable" should {
        "reject the request and return 400" in new StocksBulkUpdateFSpecContext {
          val variant1Stock = Factory.stock(variant1London, quantity = Some(3)).create
          val variant2Stock = Factory.stock(variant2London, quantity = Some(4)).create
          val updates = {
            val stockUpdates = random[StockUpdate](2)
            val stockUpdate1 =
              stockUpdates(0).copy(locationId = london.id, productId = template.id, sellOutOfStock = Some(true))
            val stockUpdate2 =
              stockUpdates(1).copy(locationId = london.id, productId = variant2.id, sellOutOfStock = Some(true))
            Seq(stockUpdate1, stockUpdate2)
          }

          Post(s"/v1/stocks.bulk_update", updates).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
      "if a product-location ids are duplicated" should {
        "reject the request and return 400" in new StocksBulkUpdateFSpecContext {
          val variant1Stock = Factory.stock(variant1London, quantity = Some(3)).create
          val updates = {
            val stockUpdates = random[StockUpdate](2)
            val stockUpdate1 =
              stockUpdates(0).copy(locationId = london.id, productId = variant1.id, sellOutOfStock = Some(true))
            val stockUpdate2 =
              stockUpdates(1).copy(locationId = london.id, productId = variant1.id, sellOutOfStock = Some(true))
            Seq(stockUpdate1, stockUpdate2)
          }

          Post(s"/v1/stocks.bulk_update", updates).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
      "if a stock doesn't belong to current user's merchant" should {
        "not update that stock and not update the others" in new StocksBulkUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorProductLocation =
            Factory.productLocation(competitorProduct, competitorLocation).create
          val competitorStock = Factory.stock(competitorProductLocation).create

          val myInitialStocksRowsCount = stockDao.findAllByMerchantId(merchant.id).await.size
          val competitorInitialStocksRowsCount = stockDao.findAllByMerchantId(competitor.id).await.size

          val updates = {
            val stockUpdates = random[StockUpdate](2)
            val stockUpdate1 = stockUpdates(0).copy(
              locationId = london.id,
              productId = competitorProduct.id,
              sellOutOfStock = Some(true),
            )
            val stockUpdate2 =
              stockUpdates(1).copy(locationId = london.id, productId = variant2.id, sellOutOfStock = Some(true))
            Seq(stockUpdate1, stockUpdate2)
          }

          Post(s"/v1/stocks.bulk_update", updates).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            competitorStock ==== stockDao.findById(competitorStock.id).await.get
            myInitialStocksRowsCount ==== stockDao.findAllByMerchantId(merchant.id).await.size
            competitorInitialStocksRowsCount ==== stockDao.findAllByMerchantId(competitor.id).await.size
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new StocksBulkUpdateFSpecContext {
        Post(s"/v1/stocks.bulk_update", Seq.empty).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
