package io.paytouch.core.reports.resources.products

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.resources.ReportsFSpec
import io.paytouch.core.reports.views.ProductView
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

class ProductsTopFSpec extends ReportsFSpec[ProductView] {

  def action = "top"

  def view = ProductView(MockedRestApi.variantService)

  val fixtures = new ProductsTopFSpecContext

  class ProductsTopFSpecContext extends ReportsFSpecContext with ProductsFSpecFixtures {
    val productATop = ProductTop(productA.id, productA.name, 1002, 31.$$$, 45.$$$, 25.$$$, 80.6452, None)
    val productBTop = ProductTop(productB.id, productB.name, 205, 1493.90.$$$, 1502.$$$, 493.4.$$$, 33.0276, None)
    val productCTop = ProductTop(productC.id, productC.name, 3, 2.$$$, 5.$$$, -1.$$$, -50.0000, None)
    val productDTop = ProductTop(productD.id, productD.name, 5, -8.$$$, 0.$$$, -13.00.$$$, 162.5000, None)
    val variantProductATop = ProductTop(
      variantProductA.id,
      variantProductA.name,
      5,
      -1.$$$,
      0.$$$,
      -6.00.$$$,
      600.0000,
      Some(Seq(variantOption1Entity)),
    )
  }

  "GET /v1/reports/products.top" in {

    "with no order_by it should reject the request" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval")
        .addHeader(authorizationHeader) ~> routes ~> check {
        rejection ==== MissingQueryParamRejection("order_by[]")
      }
    }

    "when no items are found" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$emptyParams&order_by[]=id")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedResult = buildExpectedResultWhenNoInterval[ProductTop](emptyFrom, emptyTo)
        result ==== expectedResult
      }
    }

    "with order by id" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=id")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedProductTops = Seq(
          productATop,
          productBTop,
          productCTop,
          productDTop,
          variantProductATop,
        )
        val expectedResult = buildExpectedResultWhenNoInterval(expectedProductTops.sortBy(_.id.toString): _*)
        result ==== expectedResult
      }
    }

    "with order by name" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=name")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          productATop,
          productBTop,
          productCTop,
          productDTop,
          variantProductATop,
        )
        result ==== expectedResult
      }
    }

    "with order by quantity sold" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=quantity&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          productATop,
          productBTop,
        )
        result ==== expectedResult
      }
    }

    "with order by revenue" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=revenue&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          productBTop,
          productATop,
        )
        result ==== expectedResult
      }
    }

    "with order by profit" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=profit&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          productBTop,
          productATop,
        )
        result ==== expectedResult
      }
    }

    "with order by margin" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=margin&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          variantProductATop,
          productDTop,
        )
        result ==== expectedResult
      }
    }

    "if order items have tax_amount = NULL" should {
      "with order by quantity sold" in new ProductsTopFSpecContext {
        Factory
          .orderItem(
            order1,
            product = Some(productB),
            quantity = Some(2000),
            costAmount = Some(5),
            taxAmount = None,
            totalPriceAmount = Some(15000),
          )
          .create
        override val productBTop =
          ProductTop(productB.id, productB.name, 2205, 16493.90.$$$, 16502.$$$, 5493.4.$$$, 33.3056, None)

        Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=quantity&n=1")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ProductTop]]
          val expectedResult = buildExpectedResultWhenNoInterval(
            productBTop,
          )
          result ==== expectedResult
        }
      }
    }

    "filtered by location_id" in new ProductsTopFSpecContext {
      Get(s"/v1/reports/products.top?$defaultParamsNoInterval&order_by[]=id&location_id=${london.id}")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[ProductTop]]
        val expectedProductTops = Seq(
          ProductTop(productA.id, productA.name, 2, 24.$$$, 30.$$$, 18.$$$, 75, None),
          ProductTop(productB.id, productB.name, 200, 1492.00.$$$, 1500.$$$, 492.0.$$$, 32.9759, None),
        )
        val expectedResult = buildExpectedResultWhenNoInterval(expectedProductTops.sortBy(_.id.toString): _*)
        result ==== expectedResult
      }
    }
  }
}
