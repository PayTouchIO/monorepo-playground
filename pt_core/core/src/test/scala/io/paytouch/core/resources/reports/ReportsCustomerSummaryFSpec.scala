package io.paytouch.core.resources.reports

import java.time.ZonedDateTime
import java.util.Currency

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ MultipleLocationFixtures, FixtureDaoFactory => Factory }

class ReportsCustomerSummaryFSpec extends ReportsFSpec {

  abstract class ReportsCustomerSummaryFSpecContext extends ReportResourceFSpecContext {
    val emptyCustomersSummary = CustomersSummary(count = 0, spend = MonetaryAmount(0, currency))
  }

  "GET /v1/reports.customers_summary" in {
    "if request has valid token" in {
      "with no parameters" should {
        "compute the report customer summary for all accessible orders" in new ReportsCustomerSummaryFSpecContext
          with Fixtures {
          Get(s"/v1/reports.customers_summary").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val customerSummary = responseAs[ApiResponse[Map[Currency, ReportCustomerSummary]]].data
            customerSummary.keySet ==== Set(currency)
            customerSummary(currency) ==== ReportCustomerSummary(
              `new` = CustomersSummary(count = 2, spend = 60.$$$),
              returning = emptyCustomersSummary,
            )
          }
        }
      }

      "filtered by location id" in {

        "if the location is accessible by the user" should {
          "compute the report customer summary for all orders belonging to a location" in new ReportsCustomerSummaryFSpecContext
            with Fixtures {
            Get(s"/v1/reports.customers_summary?location_id=${rome.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val customerSummary = responseAs[ApiResponse[Map[Currency, ReportCustomerSummary]]].data
              customerSummary.keySet ==== Set(currency)
              customerSummary(currency) ==== ReportCustomerSummary(
                `new` = CustomersSummary(count = 2, spend = 30.$$$),
                returning = emptyCustomersSummary,
              )
            }
          }
        }

        "if the location is non accessible by the user" should {
          "return empty data" in new ReportsCustomerSummaryFSpecContext with Fixtures {
            Get(s"/v1/reports.customers_summary?location_id=${newYork.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val customerSummary = responseAs[ApiResponse[Map[Currency, ReportCustomerSummary]]].data
              customerSummary ==== Map(currency -> ReportCustomerSummary(emptyCustomersSummary, emptyCustomersSummary))
            }
          }
        }
      }

      "filtered by from" should {
        "compute the report customer summary for all orders received after some time" in new ReportsCustomerSummaryFSpecContext
          with Fixtures {
          Get(s"/v1/reports.customers_summary?from=$localDateTime").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val customerSummary = responseAs[ApiResponse[Map[Currency, ReportCustomerSummary]]].data
            customerSummary.keySet ==== Set(currency)
            customerSummary(currency) ==== ReportCustomerSummary(
              `new` = CustomersSummary(count = 1, spend = 20.$$$),
              returning = CustomersSummary(count = 1, spend = 30.$$$),
            )
          }
        }
      }

      "filtered by to" should {
        "compute the report customer summary for all orders received before some time" in new ReportsCustomerSummaryFSpecContext
          with Fixtures {
          Get(s"/v1/reports.customers_summary?to=$localDateTime").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val customerSummary = responseAs[ApiResponse[Map[Currency, ReportCustomerSummary]]].data
            customerSummary.keySet ==== Set(currency)
            customerSummary(currency) ==== ReportCustomerSummary(
              `new` = CustomersSummary(count = 1, spend = 10.$$$),
              returning = emptyCustomersSummary,
            )
          }
        }
      }

      "filtered by from and to" should {
        "compute the report customer summary for all orders received in a certain period" in new ReportsCustomerSummaryFSpecContext {
          val fromLocalDateTime = "2016-09-03T00:00:00"
          val toLocalDateTime = "2016-09-04T00:00:00"

          val date = ZonedDateTime.parse("2016-09-03T11:15:30+01:00[Europe/Rome]")

          val daniela = {
            val globalCustomer = Factory.globalCustomer().create
            Factory.customerMerchant(merchant, globalCustomer).create
          }

          val francesco = {
            val globalCustomer = Factory.globalCustomer().create
            Factory.customerMerchant(merchant, globalCustomer).create
          }

          Factory
            .order(
              merchant,
              location = Some(rome),
              paymentStatus = Some(PaymentStatus.Paid),
              isInvoice = Some(false),
              totalAmount = Some(10),
              receivedAt = Some(date),
              customer = Some(francesco),
            )
            .create

          Factory
            .order(
              merchant,
              location = Some(rome),
              paymentStatus = Some(PaymentStatus.Paid),
              isInvoice = Some(false),
              totalAmount = Some(20),
              receivedAt = Some(date.minusWeeks(1)),
              customer = Some(francesco),
            )
            .create

          Factory
            .order(
              merchant,
              location = Some(london),
              paymentStatus = Some(PaymentStatus.Paid),
              isInvoice = Some(false),
              totalAmount = Some(30),
              receivedAt = Some(date.minusMonths(1)),
              customer = Some(daniela),
            )
            .create

          Get(s"/v1/reports.customers_summary?from=$fromLocalDateTime&to=$toLocalDateTime")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val customerSummary = responseAs[ApiResponse[Map[Currency, ReportCustomerSummary]]].data
            customerSummary.keySet ==== Set(currency)
            customerSummary(currency) ==== ReportCustomerSummary(
              `new` = emptyCustomersSummary,
              returning = CustomersSummary(count = 1, spend = 10.$$$),
            )
          }
        }
      }
    }
  }

  trait Fixtures extends MultipleLocationFixtures {

    val localDateTime = "2015-12-03T00:00:00"
    val dateInRome = ZonedDateTime.parse("2015-12-03T01:15:30+01:00[Europe/Rome]")

    val daniela = {
      val globalCustomer = Factory.globalCustomer().create
      Factory.customerMerchant(merchant, globalCustomer).create
    }

    val francesco = {
      val globalCustomer = Factory.globalCustomer().create
      Factory.customerMerchant(merchant, globalCustomer).create
    }

    val newYork = Factory.location(merchant).create

    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(10),
        receivedAt = Some(dateInRome.minusMonths(1)),
        customer = Some(daniela),
      )
      .create

    Factory
      .order(
        merchant,
        location = Some(rome),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(20),
        receivedAt = Some(dateInRome),
        customer = Some(francesco),
      )
      .create

    Factory
      .order(
        merchant,
        location = Some(london),
        paymentStatus = Some(PaymentStatus.Paid),
        isInvoice = Some(false),
        totalAmount = Some(30),
        receivedAt = Some(dateInRome),
        customer = Some(daniela),
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
        customer = Some(daniela),
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
        customer = Some(daniela),
      )
      .create
  }
}
