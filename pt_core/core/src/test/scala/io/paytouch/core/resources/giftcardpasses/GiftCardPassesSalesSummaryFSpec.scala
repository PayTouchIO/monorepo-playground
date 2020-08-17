package io.paytouch.core.resources.giftcardpasses

import java.time.LocalDate

import io.paytouch.core.entities._
import io.paytouch.core.reports.resources.giftcardpasses.GiftCardPassesFSpecFixtures
import io.paytouch.core.utils.{ FSpec, FixtureDaoFactory => Factory }

class GiftCardPassesSalesSummaryFSpec extends FSpec {

  abstract class GiftCardPassesSalesSummaryFSpecContext extends FSpecContext with GiftCardPassesFSpecFixtures {

    val localWeek1 = week1.toLocalDate
    val localWeek2 = week2.toLocalDate
    val localWeek3 = week3.toLocalDate
    val emptyWeek = LocalDate.of(2010, 10, 9)

  }

  "GET /v1/gift_card_passes.sales_summary" in {
    "if request has valid token" should {

      "with no parameters" should {
        "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
          val expectedPurchased =
            GiftCardPassSalesReport(count = 4, customers = 4, value = MonetaryAmount(210, currency))
          val expectedUsed = GiftCardPassSalesReport(count = 3, customers = 3, value = MonetaryAmount(55, currency))
          val expectedUnused = GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(155, currency))

          Get(s"/v1/gift_card_passes.sales_summary").addHeader(authorizationHeader) ~> routes ~> check {
            val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
            expectedPurchased ==== summary.purchased
            expectedUsed ==== summary.used
            expectedUnused ==== summary.unused
          }
        }
      }

      "with location id filter" should {
        "return a sales summary for gift cards for non-accessible location" in new GiftCardPassesSalesSummaryFSpecContext {
          val newYork = Factory.location(merchant).create

          val emptyReport = GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(0, currency))

          Get(s"/v1/gift_card_passes.sales_summary?location_id=${newYork.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
            emptyReport ==== summary.purchased
            emptyReport ==== summary.used
            emptyReport ==== summary.unused
          }
        }

        "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
          val expectedPurchased =
            GiftCardPassSalesReport(count = 2, customers = 2, value = MonetaryAmount(90, currency))
          val expectedUsed = GiftCardPassSalesReport(count = 2, customers = 3, value = MonetaryAmount(35, currency))
          val expectedUnused = GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(55, currency))

          Get(s"/v1/gift_card_passes.sales_summary?location_id=${willow1.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
            expectedPurchased ==== summary.purchased
            expectedUsed ==== summary.used
            expectedUnused ==== summary.unused
          }
        }
      }

      "with from filter" should {
        "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
          val expectedPurchased =
            GiftCardPassSalesReport(count = 2, customers = 2, value = MonetaryAmount(140, currency))
          val expectedUsed = GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(5, currency))
          val expectedUnused = GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(135, currency))

          Get(s"/v1/gift_card_passes.sales_summary?from=$localWeek2")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
            expectedPurchased ==== summary.purchased
            expectedUsed ==== summary.used
            expectedUnused ==== summary.unused
          }
        }
      }

      "with to filter" should {
        "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
          val expectedPurchased =
            GiftCardPassSalesReport(count = 2, customers = 2, value = MonetaryAmount(70, currency))
          val expectedUsed = GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(25, currency))
          val expectedUnused = GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(45, currency))

          Get(s"/v1/gift_card_passes.sales_summary?to=$localWeek2")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
            expectedPurchased ==== summary.purchased
            expectedUsed ==== summary.used
            expectedUnused ==== summary.unused
          }
        }
      }

      "with from-to filter" should {
        "week 1 only" should {
          "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
            val expectedPurchased =
              GiftCardPassSalesReport(count = 2, customers = 2, value = MonetaryAmount(70, currency))
            val expectedUsed = GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(25, currency))
            val expectedUnused =
              GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(45, currency))

            Get(s"/v1/gift_card_passes.sales_summary?from=$localWeek1&to=$localWeek2")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
              expectedPurchased ==== summary.purchased
              expectedUsed ==== summary.used
              expectedUnused ==== summary.unused
            }
          }
        }

        "week 2 only" should {
          "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
            val expectedPurchased =
              GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(40, currency))
            val expectedUsed = GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(0, currency))
            val expectedUnused =
              GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(40, currency))

            Get(s"/v1/gift_card_passes.sales_summary?from=$localWeek2&to=$localWeek3")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
              expectedPurchased ==== summary.purchased
              expectedUsed ==== summary.used
              expectedUnused ==== summary.unused
            }
          }
        }

        "week 3 only" should {
          "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
            val expectedPurchased =
              GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(100, currency))
            val expectedUsed = GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(0, currency))
            val expectedUnused =
              GiftCardPassSalesReport(count = 1, customers = 1, value = MonetaryAmount(100, currency))

            Get(s"/v1/gift_card_passes.sales_summary?from=$localWeek3&to=${localWeek3.plusWeeks(1)}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
              expectedPurchased ==== summary.purchased
              expectedUsed ==== summary.used
              expectedUnused ==== summary.unused
            }
          }
        }

        "empty week" should {
          "return a sales summary for gift cards" in new GiftCardPassesSalesSummaryFSpecContext {
            val expectedPurchased =
              GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(0, currency))
            val expectedUsed = GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(0, currency))
            val expectedUnused =
              GiftCardPassSalesReport(count = 0, customers = 0, value = MonetaryAmount(0, currency))

            Get(s"/v1/gift_card_passes.sales_summary?from=$emptyWeek&to=${emptyWeek.plusWeeks(1)}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val summary = responseAs[ApiResponse[GiftCardPassSalesSummary]].data
              expectedPurchased ==== summary.purchased
              expectedUsed ==== summary.used
              expectedUnused ==== summary.unused
            }
          }
        }
      }
    }
  }

}
