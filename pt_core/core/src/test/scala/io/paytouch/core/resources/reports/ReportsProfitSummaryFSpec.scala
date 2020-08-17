package io.paytouch.core.resources.reports

import java.time.ZonedDateTime
import java.util.Currency

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ MultipleLocationFixtures, FixtureDaoFactory => Factory }

class ReportsProfitSummaryFSpec extends ReportsFSpec {

  abstract class ReportsProfitSummaryFSpecContext extends ReportResourceFSpecContext

  "GET /v1/reports.profit_summary" in {
    "if request has valid token" in {
      "with no parameters" should {
        "compute the report profit summary for all accessible orders" in new ReportsProfitSummaryFSpecContext
          with Fixtures {
          Get(s"/v1/reports.profit_summary").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val profitSummary = responseAs[ApiResponse[Map[Currency, ReportProfitSummary]]].data
            profitSummary.keySet ==== Set(currency)
            profitSummary(currency) ==== ReportProfitSummary(
              profit = 60.$$$,
              profitPreviousWeek = None,
              profitPreviousMonth = None,
            )
          }
        }
      }

      "filtered by location id" in {

        "if the location is accessible by the user" should {
          "compute the report profit summary for all orders belonging to a location" in new ReportsProfitSummaryFSpecContext
            with Fixtures {
            Get(s"/v1/reports.profit_summary?location_id=${rome.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val profitSummary = responseAs[ApiResponse[Map[Currency, ReportProfitSummary]]].data
              profitSummary.keySet ==== Set(currency)
              profitSummary(currency) ==== ReportProfitSummary(
                profit = 30.$$$,
                profitPreviousWeek = None,
                profitPreviousMonth = None,
              )
            }
          }
        }

        "if the location is non accessible by the user" should {
          "return empty data" in new ReportsProfitSummaryFSpecContext with Fixtures {
            Get(s"/v1/reports.profit_summary?location_id=${newYork.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val profitSummary = responseAs[ApiResponse[Map[Currency, ReportProfitSummary]]].data
              val emptyMonetaryAmm = MonetaryAmount(0, currency)
              profitSummary ==== Map(currency -> ReportProfitSummary(emptyMonetaryAmm, None, None))
            }
          }
        }
      }

      "filtered by from" should {
        "compute the report profit summary for all orders received after some time" in new ReportsProfitSummaryFSpecContext
          with Fixtures {
          Get(s"/v1/reports.profit_summary?from=$localDateTime").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val profitSummary = responseAs[ApiResponse[Map[Currency, ReportProfitSummary]]].data
            profitSummary.keySet ==== Set(currency)
            profitSummary(currency) ==== ReportProfitSummary(
              profit = 30.$$$,
              profitPreviousWeek = None,
              profitPreviousMonth = None,
            )
          }
        }
      }

      "filtered by to" should {
        "compute the report profit summary for all orders received before some time" in new ReportsProfitSummaryFSpecContext
          with Fixtures {
          Get(s"/v1/reports.profit_summary?to=$localDateTime").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val profitSummary = responseAs[ApiResponse[Map[Currency, ReportProfitSummary]]].data
            profitSummary.keySet ==== Set(currency)
            profitSummary(currency) ==== ReportProfitSummary(
              profit = 30.$$$,
              profitPreviousWeek = None,
              profitPreviousMonth = None,
            )
          }
        }
      }

      "filtered by from and to" should {
        "compute the report profit summary for all orders received in a certain period" in new ReportsProfitSummaryFSpecContext {
          val fromLocalDateTime = "2016-09-03T00:00:00"
          val toLocalDateTime = "2016-09-04T00:00:00"

          val date = ZonedDateTime.parse("2016-09-03T11:15:30+01:00[Europe/Rome]")

          Factory
            .order(
              merchant,
              location = Some(rome),
              paymentStatus = Some(PaymentStatus.Paid),
              isInvoice = Some(false),
              subtotalAmount = Some(12),
              discountAmount = Some(2),
              receivedAt = Some(date),
            )
            .create

          Factory
            .order(
              merchant,
              location = Some(rome),
              paymentStatus = Some(PaymentStatus.Paid),
              isInvoice = Some(false),
              subtotalAmount = Some(20),
              discountAmount = Some(0),
              receivedAt = Some(date.minusWeeks(1)),
            )
            .create

          Factory
            .order(
              merchant,
              location = Some(london),
              paymentStatus = Some(PaymentStatus.Paid),
              isInvoice = Some(false),
              subtotalAmount = Some(45),
              discountAmount = Some(15),
              receivedAt = Some(date.minusMonths(1)),
            )
            .create

          Get(s"/v1/reports.profit_summary?from=$fromLocalDateTime&to=$toLocalDateTime")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val profitSummary = responseAs[ApiResponse[Map[Currency, ReportProfitSummary]]].data
            profitSummary.keySet ==== Set(currency)
            profitSummary(currency) ==== ReportProfitSummary(
              profit = 10.$$$,
              profitPreviousWeek = Some(20.$$$),
              profitPreviousMonth = Some(30.$$$),
            )
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
        subtotalAmount = Some(12),
        discountAmount = Some(2),
        receivedAt = Some(dateInRome.plusDays(1)),
      )
      .create

    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        subtotalAmount = Some(20),
        discountAmount = Some(0),
        receivedAt = Some(dateInRome.plusDays(2)),
      )
      .create

    Factory
      .order(
        merchant,
        location = Some(london),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        subtotalAmount = Some(45),
        discountAmount = Some(15),
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
        subtotalAmount = Some(12),
        discountAmount = Some(2),
      )
      .create

    // to be ignored as payment status not in range
    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Voided),
        isInvoice = Some(false),
        subtotalAmount = Some(12),
        discountAmount = Some(2),
      )
      .create

    // to be ignored as isInvoice is true
    Factory
      .order(
        merchant,
        location = Some(london),
        paymentStatus = Some(PaymentStatus.Voided),
        isInvoice = Some(true),
        subtotalAmount = Some(12),
        discountAmount = Some(2),
      )
      .create
  }
}
