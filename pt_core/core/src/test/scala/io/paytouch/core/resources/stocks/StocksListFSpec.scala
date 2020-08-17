package io.paytouch.core.resources.stocks

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class StocksListFSpec extends StocksFSpec {

  abstract class StocksListFSpecContext extends StockResourceFSpecContext

  "GET /v1/stocks.list?product_id=$" in {
    "if request has valid token" in {
      "without product id" in {
        "returns all stocks for simple and variant products" in new StocksListFSpecContext {
          val simpleStock = Factory.stock(simpleLondon).create

          val templateLondon = Factory.productLocation(template, london).create
          val templateStock = Factory.stock(templateLondon).create

          val variant1LondonStock = Factory.stock(variant1London).create
          val variant2LondonStock = Factory.stock(variant2London).create

          Get(s"/v1/stocks.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val stocks = responseAs[PaginatedApiResponse[Seq[Stock]]].data
            val responseProductLocationPairs = stocks.map(stock => (stock.locationId, stock.productId))
            val expectedProductLocationPairs = Seq(
              (simpleLondon.locationId, simpleLondon.productId),
              (variant1London.locationId, variant1London.productId),
              (variant2London.locationId, variant2London.productId),
            )
            responseProductLocationPairs must containTheSameElementsAs(expectedProductLocationPairs)
            assertBulkResponse(stocks)
          }
        }
      }

      "if product id is a main product" in {
        "with no params" should {
          "return all stocks for all variant/location pairs" in new StocksListFSpecContext {
            val variant1LondonStock = Factory.stock(variant1London).create
            val variant2LondonStock = Factory.stock(variant2London).create

            Get(s"/v1/stocks.list?product_id=${template.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val stocks = responseAs[PaginatedApiResponse[Seq[Stock]]].data
              val responseProductLocationPairs = stocks.map(stock => (stock.locationId, stock.productId))
              val expectedProductLocationPairs = Seq(
                (variant1London.locationId, variant1London.productId),
                (variant2London.locationId, variant2London.productId),
              )
              responseProductLocationPairs must containTheSameElementsAs(expectedProductLocationPairs)
              assertBulkResponse(stocks)
            }
          }
        }
        "with location id filter" should {
          "return all stocks for all variant after given time" in new StocksListFSpecContext {
            val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

            val variant1Rome = Factory.productLocation(variant1, rome).create
            val variant2Rome = Factory.productLocation(variant2, rome).create

            val variant1LondonStock = Factory.stock(variant1London, overrideNow = Some(now.plusDays(1))).create
            val variant2LondonStock = Factory.stock(variant2London, overrideNow = Some(now.minusDays(1))).create
            val variant1RomeStock = Factory.stock(variant1Rome, overrideNow = Some(now.plusDays(1))).create
            val variant2RomeStock = Factory.stock(variant2Rome, overrideNow = Some(now.minusDays(1))).create

            Get(s"/v1/stocks.list?updated_since=2015-12-03")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val stocks = responseAs[PaginatedApiResponse[Seq[Stock]]].data
              val responseProductLocationPairs = stocks.map(stock => (stock.locationId, stock.productId))
              val expectedProductLocationPairs =
                Seq(variant1LondonStock, variant1RomeStock).map(stock => (stock.locationId, stock.productId))
              responseProductLocationPairs must containTheSameElementsAs(expectedProductLocationPairs)
              assertBulkResponse(stocks)
            }
          }
        }
      }
      "if product id is a product variant" should {
        "return all stocks of the variant for all locations" in new StocksListFSpecContext {
          val variant1Rome = Factory.productLocation(variant1, rome).create

          val variant1LondonStock = Factory.stock(variant1London).create
          val variant1RomeStock = Factory.stock(variant1Rome).create

          Get(s"/v1/stocks.list?product_id=${variant1.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val stocks = responseAs[PaginatedApiResponse[Seq[Stock]]].data
            val responseProductLocationPairs = stocks.map(stock => (stock.locationId, stock.productId))
            val expectedProductLocationPairs = Seq(
              (variant1London.locationId, variant1London.productId),
              (variant1Rome.locationId, variant1Rome.productId),
            )
            responseProductLocationPairs must containTheSameElementsAs(expectedProductLocationPairs)
            assertBulkResponse(stocks)
          }
        }
      }
      "if product doesn't belong to same merchant" in {
        "return an empty list" in new StocksListFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          Get(s"/v1/stocks.list?product_id=${competitorProduct.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val stocks = responseAs[PaginatedApiResponse[Seq[Stock]]].data
            stocks ==== Seq.empty
          }
        }
      }
    }
  }
}
