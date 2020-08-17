package io.paytouch.core.resources.inventories

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.resources.products.ProductsFSpec
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductsComputeRevenueFSpec extends ProductsFSpec {

  abstract class ProductsComputeRevenueFSpecContext extends ProductResourceFSpecContext

  "GET /v1/products.compute_revenue?product_id=<product-id>" in {

    "if request has valid token" in {

      "if product belongs to same merchant" in {

        "with no parameters" should {

          "return empty list if the product does not have order items" in new ProductsComputeRevenueFSpecContext {
            val product = Factory.simpleProduct(merchant).create

            Get(s"/v1/products.compute_revenue?product_id=${product.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productRevenue = responseAs[ApiResponse[ProductRevenue]].data

              productRevenue.productId ==== product.id
              productRevenue.revenuePerLocation ==== Seq.empty
            }
          }

          "return the product revenue for each location for simple product" in new ProductsComputeRevenueFSpecContext {
            val product = Factory.simpleProduct(merchant).create

            val orderA = Factory.order(merchant, Some(london)).create
            Factory
              .orderItem(
                order = orderA,
                product = Some(product),
                quantity = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(100),
                costAmount = Some(10),
                discountAmount = Some(2),
                taxAmount = Some(2),
              )
              .create

            val orderB = Factory.order(merchant, Some(rome)).create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(10),
                costAmount = Some(1),
                discountAmount = Some(1),
                taxAmount = Some(1),
              )
              .create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(4),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(25),
                costAmount = Some(7),
                discountAmount = Some(0.1),
                taxAmount = Some(2),
              )
              .create

            Factory.orderItem(order = orderB, product = None).create

            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(4),
                paymentStatus = Some(PaymentStatus.Pending),
                totalPriceAmount = Some(25),
                costAmount = Some(7),
                discountAmount = Some(0.1),
                taxAmount = Some(2),
              )
              .create

            Get(s"/v1/products.compute_revenue?product_id=${product.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productRevenue = responseAs[ApiResponse[ProductRevenue]].data

              productRevenue.productId ==== product.id
              productRevenue.revenuePerLocation.map(_.locationId) should containTheSameElementsAs(
                Seq(rome.id, london.id),
              )

              val londonRevenue = productRevenue.revenuePerLocation.find(_.locationId == london.id).get
              londonRevenue.avgPrice ==== (12.34.$$$)
              londonRevenue.avgDiscount ==== (2.$$$)
              londonRevenue.avgCost ==== (10.$$$)
              londonRevenue.avgMargin ==== 18.96
              londonRevenue.totalSold.amount ==== 20
              londonRevenue.totalRevenue ==== (206.8.$$$)
              londonRevenue.totalTax ==== (40.$$$)
              londonRevenue.totalProfit ==== (-104.$$$)

              val romeRevenue = productRevenue.revenuePerLocation.find(_.locationId == rome.id).get
              romeRevenue.avgPrice ==== (12.34.$$$)
              romeRevenue.avgDiscount ==== (0.4.$$$)
              romeRevenue.avgCost ==== (5.$$$)
              romeRevenue.avgMargin ==== 59.48
              romeRevenue.totalSold.amount ==== 6
              romeRevenue.totalRevenue ==== (71.64.$$$)
              romeRevenue.totalTax ==== (10.$$$)
              romeRevenue.totalProfit ==== (0.9.$$$)
            }
          }

          "return the product revenue for each location for template product" in new ProductsComputeRevenueFSpecContext {
            val template = Factory.templateProduct(merchant).create
            val variantProduct = Factory.variantProduct(merchant, template).create

            val orderA = Factory.order(merchant, Some(london)).create
            Factory
              .orderItem(
                order = orderA,
                product = Some(variantProduct),
                quantity = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(100),
                costAmount = Some(10),
                discountAmount = Some(2),
                taxAmount = Some(2),
              )
              .create

            val orderB = Factory.order(merchant, Some(rome)).create
            Factory
              .orderItem(
                order = orderB,
                product = Some(variantProduct),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(10),
                costAmount = Some(1),
                discountAmount = Some(1),
                taxAmount = Some(1),
              )
              .create
            Factory
              .orderItem(
                order = orderB,
                product = Some(variantProduct),
                quantity = Some(4),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(25),
                costAmount = Some(7),
                discountAmount = Some(0.1),
                taxAmount = Some(2),
              )
              .create
            Factory.orderItem(order = orderB, product = None).create

            Get(s"/v1/products.compute_revenue?product_id=${template.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productRevenue = responseAs[ApiResponse[ProductRevenue]].data

              productRevenue.productId ==== template.id
              productRevenue.revenuePerLocation.map(_.locationId) should containTheSameElementsAs(
                Seq(rome.id, london.id),
              )

              val londonRevenue = productRevenue.revenuePerLocation.find(_.locationId == london.id).get
              londonRevenue.avgPrice ==== (12.34.$$$)
              londonRevenue.avgDiscount ==== (2.$$$)
              londonRevenue.avgCost ==== (10.$$$)
              londonRevenue.avgMargin ==== 18.96
              londonRevenue.totalSold.amount ==== 20
              londonRevenue.totalRevenue ==== (206.80.$$$)
              londonRevenue.totalTax ==== (40.$$$)
              londonRevenue.totalProfit ==== (-104.$$$)

              val romeRevenue = productRevenue.revenuePerLocation.find(_.locationId == rome.id).get
              romeRevenue.avgPrice ==== (12.34.$$$)
              romeRevenue.avgDiscount ==== (0.4.$$$)
              romeRevenue.avgCost ==== (5.$$$)
              romeRevenue.avgMargin ==== 59.48
              romeRevenue.totalSold.amount ==== 6
              romeRevenue.totalRevenue ==== (71.64.$$$)
              romeRevenue.totalTax ==== (10.$$$)
              romeRevenue.totalProfit ==== (0.9.$$$)
            }
          }

          "return the product revenue only for the locations accessible to the user" in new ProductsComputeRevenueFSpecContext {
            val newYork = Factory.location(merchant).create

            val product = Factory.simpleProduct(merchant).create

            val orderA = Factory.order(merchant, Some(london)).create
            Factory
              .orderItem(
                order = orderA,
                product = Some(product),
                quantity = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(100),
                costAmount = Some(10),
                discountAmount = Some(2),
                taxAmount = Some(2),
              )
              .create

            val orderB = Factory.order(merchant, Some(newYork)).create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(100),
                costAmount = Some(7),
                discountAmount = Some(2),
                taxAmount = Some(2),
              )
              .create

            Get(s"/v1/products.compute_revenue?product_id=${product.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productRevenue = responseAs[ApiResponse[ProductRevenue]].data

              productRevenue.productId ==== product.id
              productRevenue.revenuePerLocation.map(_.locationId) ==== Seq(london.id)

              val londonRevenue = productRevenue.revenuePerLocation.find(_.locationId == london.id).get
              londonRevenue.avgPrice ==== (12.34.$$$)
              londonRevenue.avgDiscount ==== (2.$$$)
              londonRevenue.avgCost ==== (10.$$$)
              londonRevenue.avgMargin ==== 18.96
              londonRevenue.totalSold.amount ==== 20
              londonRevenue.totalRevenue ==== (206.8.$$$)
              londonRevenue.totalTax ==== (40.$$$)
              londonRevenue.totalProfit ==== (-104.$$$)
            }
          }
        }

        "filtered by from date-time" should {
          "return a product revenue that consider only orders after the given date" in new ProductsComputeRevenueFSpecContext {
            val dateTimeInRome = ZonedDateTime.parse("2015-12-03T01:15:30+01:00[Europe/Rome]")
            val dateTimeInLondon = ZonedDateTime.parse("2015-12-03T23:59:30-10:00[Pacific/Honolulu]")
            val product = Factory.simpleProduct(merchant).create

            val orderA = Factory.order(merchant, Some(london), receivedAt = Some(dateTimeInLondon)).create
            Factory
              .orderItem(
                order = orderA,
                product = Some(product),
                quantity = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(100),
                costAmount = Some(10),
                discountAmount = Some(2),
                taxAmount = Some(2),
              )
              .create

            val orderB = Factory.order(merchant, Some(rome), receivedAt = Some(dateTimeInRome.minusDays(1))).create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(10),
                costAmount = Some(1),
                discountAmount = Some(1),
                taxAmount = Some(1),
              )
              .create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(4),
                totalPriceAmount = Some(25),
                costAmount = Some(7),
                discountAmount = Some(0.1),
                taxAmount = Some(2),
              )
              .create
            Factory.orderItem(order = orderB, product = None).create

            Get(s"/v1/products.compute_revenue?product_id=${product.id}&from=2015-12-03T00:00:00")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productRevenue = responseAs[ApiResponse[ProductRevenue]].data

              productRevenue.productId ==== product.id
              productRevenue.revenuePerLocation.map(_.locationId) ==== Seq(london.id)

              val londonRevenue = productRevenue.revenuePerLocation.find(_.locationId == london.id).get
              londonRevenue.avgPrice ==== (12.34.$$$)
              londonRevenue.avgDiscount ==== (2.$$$)
              londonRevenue.avgCost ==== (10.$$$)
              londonRevenue.avgMargin ==== 18.96
              londonRevenue.totalSold.amount ==== 20
              londonRevenue.totalRevenue ==== (206.8.$$$)
              londonRevenue.totalTax ==== (40.$$$)
              londonRevenue.totalProfit ==== (-104.$$$)
            }
          }
        }

        "filtered by to date-time" should {
          "return a product revenue that consider only orders before the given date" in new ProductsComputeRevenueFSpecContext {
            val dateTimeInRome = ZonedDateTime.parse("2015-12-03T01:15:30+01:00[Europe/Rome]")
            val dateTimeInLondon = ZonedDateTime.parse("2015-12-03T23:59:30-10:00[Pacific/Honolulu]")
            val product = Factory.simpleProduct(merchant).create

            val orderA = Factory.order(merchant, Some(london), receivedAt = Some(dateTimeInLondon)).create
            Factory
              .orderItem(
                order = orderA,
                product = Some(product),
                quantity = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(100),
                costAmount = Some(10),
                discountAmount = Some(2),
                taxAmount = Some(2),
              )
              .create

            val orderB = Factory.order(merchant, Some(rome), receivedAt = Some(dateTimeInRome.minusDays(1))).create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(2),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(10),
                costAmount = Some(1),
                discountAmount = Some(1),
                taxAmount = Some(1),
              )
              .create
            Factory
              .orderItem(
                order = orderB,
                product = Some(product),
                quantity = Some(4),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(25),
                costAmount = Some(7),
                discountAmount = Some(0.1),
                taxAmount = Some(2),
              )
              .create
            Factory.orderItem(order = orderB, product = None).create

            Get(s"/v1/products.compute_revenue?product_id=${product.id}&to=2015-12-03T00:00:00")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val productRevenue = responseAs[ApiResponse[ProductRevenue]].data

              productRevenue.productId ==== product.id
              productRevenue.revenuePerLocation.map(_.locationId) ==== Seq(rome.id)

              val romeRevenue = productRevenue.revenuePerLocation.find(_.locationId == rome.id).get
              romeRevenue.avgPrice ==== (12.34.$$$)
              romeRevenue.avgDiscount ==== (0.4.$$$)
              romeRevenue.avgCost ==== (5.$$$)
              romeRevenue.avgMargin ==== 59.48
              romeRevenue.totalSold.amount ==== 6
              romeRevenue.totalRevenue ==== (71.64.$$$)
              romeRevenue.totalTax ==== (10.$$$)
              romeRevenue.totalProfit ==== (0.9.$$$)
            }
          }
        }
      }
      "if product doesn't belong to same merchant" in {

        "return 404" in new ProductsComputeRevenueFSpecContext {
          val competitor = Factory.merchant.create
          val competitorProduct = Factory.simpleProduct(competitor).create

          Get(s"/v1/products.compute_revenue?product_id=${competitorProduct.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
