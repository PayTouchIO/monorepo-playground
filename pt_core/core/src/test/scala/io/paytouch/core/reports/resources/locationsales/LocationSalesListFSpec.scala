package io.paytouch.core.reports.resources.locationsales

import io.paytouch.core.data.model.enums.TransactionPaymentType._
import io.paytouch.core.entities.{ MonetaryAmount, Pagination }
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.resources.ReportsListFSpec
import io.paytouch.core.reports.views.LocationSalesView
import MonetaryAmount._

class LocationSalesListFSpec extends ReportsListFSpec[LocationSalesView] {

  def view = LocationSalesView()

  class LocationSalesFSpecContext extends ReportsListFSpecContext with LocationSalesFSpecFixtures {

    def londonResult(data: SalesAggregate) =
      LocationSales(
        id = london.id,
        name = london.name,
        addressLine1 = london.addressLine1,
        data = data,
      )

    def romeResult(data: SalesAggregate) =
      LocationSales(
        id = rome.id,
        name = rome.name,
        addressLine1 = rome.addressLine1,
        data = data,
      )

    def orderedResult(londonData: SalesAggregate, romeData: SalesAggregate) =
      Seq(londonResult(londonData), romeResult(romeData))

    val totalCount = 2
  }

  val fixtures = new LocationSalesFSpecContext
  import fixtures._

  "GET /v1/reports/location_sales.list" in {
    "with default order by name" should {
      assertNoField()

      assertFieldResultWithPagination(
        "address",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "id",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "name",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "costs",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(costs = londonFullAggregate.costs),
          romeData = romeEmptyAggregate.copy(costs = romeFullAggregate.costs),
        ),
      )

      assertFieldResultWithPagination(
        "count",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate,
          romeData = romeEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "gross_sales",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(grossSales = londonFullAggregate.grossSales),
          romeData = romeEmptyAggregate.copy(grossSales = romeFullAggregate.grossSales),
        ),
      )

      assertFieldResultWithPagination(
        "net_sales",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(netSales = londonFullAggregate.netSales),
          romeData = romeEmptyAggregate.copy(netSales = romeFullAggregate.netSales),
        ),
      )

      assertFieldResultWithPagination(
        "discounts",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(discounts = londonFullAggregate.discounts),
          romeData = romeEmptyAggregate.copy(discounts = romeFullAggregate.discounts),
        ),
      )

      assertFieldResultWithPagination(
        "refunds",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(refunds = londonFullAggregate.refunds),
          romeData = romeEmptyAggregate.copy(refunds = romeFullAggregate.refunds),
        ),
      )

      assertFieldResultWithPagination(
        "taxes",
        totalCount,
        orderedResult(
          londonData = londonEmptyAggregate.copy(taxes = londonFullAggregate.taxes),
          romeData = romeEmptyAggregate.copy(taxes = romeFullAggregate.taxes),
        ),
      )

      assertFieldResultWithPagination(
        "tips",
        totalCount,
        orderedResult(
          romeData = romeEmptyAggregate.copy(tips = romeFullAggregate.tips),
          londonData = londonEmptyAggregate.copy(tips = londonFullAggregate.tips),
        ),
      )

      assertFieldResultWithPagination(
        "tender_types",
        totalCount,
        orderedResult(
          romeData = romeEmptyAggregate.copy(tenderTypes = romeFullAggregate.tenderTypes),
          londonData = londonEmptyAggregate.copy(tenderTypes = londonFullAggregate.tenderTypes),
        ),
      )

      assertAllFieldsResultWithPagination(
        totalCount,
        orderedResult(londonData = londonFullAggregate, romeData = romeFullAggregate),
      )
    }

    "with custom order" should {

      val londonThenRome = Seq(londonResult(londonFullAggregate), romeResult(romeFullAggregate))
      val romeThenLondon = Seq(romeResult(romeFullAggregate), londonResult(londonFullAggregate))

      assertOrderByResultWithPagination("id", totalCount, londonThenRome.sortBy(_.id.toString))

      assertOrderByResultWithPagination("name", totalCount, londonThenRome)

      assertOrderByResultWithPagination("count", totalCount, romeThenLondon)

      assertOrderByResultWithPagination("gross_sales", totalCount, romeThenLondon)

      assertOrderByResultWithPagination("net_sales", totalCount, romeThenLondon)

      assertOrderByResultWithPagination("discounts", totalCount, romeThenLondon)

      assertOrderByResultWithPagination("refunds", totalCount, romeThenLondon)

      assertOrderByResultWithPagination("taxes", totalCount, londonThenRome)

      assertOrderByResultWithPagination("tips", totalCount, londonThenRome)

    }

    "with pagination" should {
      assertAllFieldsResultWithPagination(
        totalCount,
        Seq(londonResult(londonFullAggregate)),
        pagination = Some(Pagination(1, 1)),
      )

    }

    "filtered by id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(londonResult(londonFullAggregate)),
        extraFilters = Some(s"id[]=${london.id}"),
      )
    }

    "filtered by order_types[]" should {
      val londonFullAggregate = londonEmptyAggregate.copy(
        costs = Some(4.50.$$$),
        discounts = Some(3.80.$$$),
        giftCardSales = Some(0.$$$),
        grossProfits = Some(-8.20.$$$),
        grossSales = Some(5.$$$),
        netSales = Some(-3.70.$$$),
        nonTaxable = Some(0.$$$),
        refunds = Some(0.$$$),
        taxable = Some(1.3.$$$),
        taxes = Some(3.70.$$$),
        tips = Some(5.00.$$$),
        tenderTypes = Some(
          Map(
            CreditCard -> MonetaryAmount(0, currency),
            DebitCard -> MonetaryAmount(5, currency),
            GiftCard -> MonetaryAmount(0, currency),
            Cash -> MonetaryAmount(0, currency),
          ),
        ),
      )

      val romeFullAggregate = romeEmptyAggregate.copy(
        count = 1,
        costs = Some(8.00.$$$),
        discounts = Some(8.$$$),
        giftCardSales = Some(0.$$$),
        grossProfits = Some(-2.3.$$$),
        grossSales = Some(10.0.$$$),
        netSales = Some(5.7.$$$),
        nonTaxable = Some(0.$$$),
        refunds = Some(41.$$$),
        taxable = Some(8.7.$$$),
        taxes = Some(1.3.$$$),
        tips = Some(3.$$$),
        tenderTypes = Some(
          Map(
            CreditCard -> MonetaryAmount(-39, currency),
            DebitCard -> MonetaryAmount(0, currency),
            GiftCard -> MonetaryAmount(1, currency),
            Cash -> MonetaryAmount(0, currency),
          ),
        ),
      )
      assertAllFieldsResultWithPagination(
        totalCount,
        orderedResult(londonData = londonFullAggregate, romeData = romeFullAggregate),
        extraFilters = Some("order_types[]=dine_in"),
      )
    }

    "filtered by location_id" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(londonResult(londonFullAggregate)),
        extraFilters = Some(s"location_id=${london.id}"),
      )
    }
  }
}
