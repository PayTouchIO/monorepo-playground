package io.paytouch.core.resources.reports

import java.time.ZonedDateTime
import java.util.Currency

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ MultipleLocationFixtures, FixtureDaoFactory => Factory }

class ReportsSalesSummaryFSpec extends ReportsFSpec {

  abstract class ReportsSalesSummaryFSpecContext extends ReportResourceFSpecContext with Fixtures

  "GET /v1/reports.sales_summary" in {
    "if request has valid token" in {
      "with no parameters" should {
        "compute the report sales summary for all accessible orders" in new ReportsSalesSummaryFSpecContext {
          Get(s"/v1/reports.sales_summary").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val salesSummary = responseAs[ApiResponse[Map[Currency, ReportSalesSummary]]].data
            salesSummary.keySet ==== Set(currency)
            salesSummary(currency) ==== ReportSalesSummary(revenue = 60.$$$, count = 3, avgSale = 20.$$$)
          }
        }
      }

      "filtered by location id" in {

        "if the location is accessible by the user" should {
          "compute the report sales summary for all orders belonging to a location" in new ReportsSalesSummaryFSpecContext {
            Get(s"/v1/reports.sales_summary?location_id=${rome.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val salesSummary = responseAs[ApiResponse[Map[Currency, ReportSalesSummary]]].data
              salesSummary.keySet ==== Set(currency)
              salesSummary(currency) ==== ReportSalesSummary(revenue = 30.$$$, count = 2, avgSale = 15.$$$)
            }
          }
        }

        "if the location is non accessible by the user" should {
          "return empty data" in new ReportsSalesSummaryFSpecContext {
            Get(s"/v1/reports.sales_summary?location_id=${newYork.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val salesSummary = responseAs[ApiResponse[Map[Currency, ReportSalesSummary]]].data
              val emptyMonetaryAmm = MonetaryAmount(0, currency)
              salesSummary ==== Map(currency -> ReportSalesSummary(emptyMonetaryAmm, 0, emptyMonetaryAmm))
            }
          }
        }
      }

      "filtered by from" should {
        "compute the report sales summary for all orders received after some time" in new ReportsSalesSummaryFSpecContext {
          Get(s"/v1/reports.sales_summary?from=$localDateTime").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val salesSummary = responseAs[ApiResponse[Map[Currency, ReportSalesSummary]]].data
            salesSummary.keySet ==== Set(currency)
            salesSummary(currency) ==== ReportSalesSummary(revenue = 30.$$$, count = 2, avgSale = 15.$$$)
          }
        }
      }

      "filtered by to" should {
        "compute the report sales summary for all orders received before some time" in new ReportsSalesSummaryFSpecContext {
          Get(s"/v1/reports.sales_summary?to=$localDateTime").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val salesSummary = responseAs[ApiResponse[Map[Currency, ReportSalesSummary]]].data
            salesSummary.keySet ==== Set(currency)
            salesSummary(currency) ==== ReportSalesSummary(revenue = 30.$$$, count = 1, avgSale = 30.$$$)
          }
        }
      }
    }
  }

  trait Fixtures extends MultipleLocationFixtures {

    val localDateTime = "2015-12-03T00:00:00"
    val dateInRome = ZonedDateTime.parse("2015-12-03T01:15:30+01:00[Europe/Rome]")

    val newYork = Factory.location(merchant).create

    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(10),
        receivedAt = Some(dateInRome.plusDays(1)),
      )
      .create
    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(20),
        receivedAt = Some(dateInRome.plusDays(2)),
      )
      .create
    Factory
      .order(
        merchant,
        location = Some(london),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(30),
        receivedAt = Some(dateInRome.minusDays(2)),
      )
      .create

    // to be ignored as location not accessible
    Factory
      .order(
        merchant,
        location = Some(newYork),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(10),
      )
      .create

    // to be ignored as payment status not in range
    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Voided),
        isInvoice = Some(false),
        totalAmount = Some(10),
      )
      .create

    // to be ignored as isInvoice is true
    Factory
      .order(
        merchant,
        location = Some(london),
        paymentStatus = Some(PaymentStatus.Voided),
        isInvoice = Some(true),
        totalAmount = Some(10),
      )
      .create
  }
}
