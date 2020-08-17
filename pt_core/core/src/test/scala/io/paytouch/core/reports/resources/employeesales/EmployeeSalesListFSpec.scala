package io.paytouch.core.reports.resources.employeesales

import io.paytouch.core.entities.Pagination
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.resources.ReportsListFSpec
import io.paytouch.core.reports.views.EmployeeSalesView
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.entities.MonetaryAmount._

class EmployeeSalesListFSpec extends ReportsListFSpec[EmployeeSalesView] {

  def view = EmployeeSalesView()

  class EmployeeSalesFSpecContext extends ReportsListFSpecContext with EmployeeSalesFSpecFixtures {

    def carloResult(data: SalesAggregate) =
      EmployeeSales(
        id = employeeCarlo.id,
        firstName = employeeCarlo.firstName,
        lastName = employeeCarlo.lastName,
        data = data,
      )

    def gabrieleResult(data: SalesAggregate) =
      EmployeeSales(
        id = employeeGabriele.id,
        firstName = employeeGabriele.firstName,
        lastName = employeeGabriele.lastName,
        data = data,
      )

    def johnResult(data: SalesAggregate) =
      EmployeeSales(
        id = user.id,
        firstName = user.firstName,
        lastName = user.lastName,
        data = data,
      )

    def orderedResult(
        carloData: SalesAggregate,
        gabrieleData: SalesAggregate,
        johnData: SalesAggregate,
      ) =
      Seq(carloResult(carloData), gabrieleResult(gabrieleData), johnResult(johnData))

    val totalCount = 3
  }

  val fixtures = new EmployeeSalesFSpecContext
  import fixtures._

  "GET /v1/reports/employee_sales.list" in {
    "with default order by name" should {
      assertNoField()

      assertFieldResultWithPagination(
        "id",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate,
          gabrieleData = gabrieleEmptyAggregate,
          johnData = johnEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "first_name",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate,
          gabrieleData = gabrieleEmptyAggregate,
          johnData = johnEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "last_name",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate,
          gabrieleData = gabrieleEmptyAggregate,
          johnData = johnEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "costs",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(costs = carloFullAggregate.costs),
          gabrieleData = gabrieleEmptyAggregate.copy(costs = gabrieleFullAggregate.costs),
          johnData = johnEmptyAggregate.copy(costs = johnFullAggregate.costs),
        ),
      )

      assertFieldResultWithPagination(
        "count",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate,
          gabrieleData = gabrieleEmptyAggregate,
          johnData = johnEmptyAggregate,
        ),
      )

      assertFieldResultWithPagination(
        "gross_sales",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(grossSales = carloFullAggregate.grossSales),
          gabrieleData = gabrieleEmptyAggregate.copy(grossSales = gabrieleFullAggregate.grossSales),
          johnData = johnEmptyAggregate.copy(grossSales = johnFullAggregate.grossSales),
        ),
      )

      assertFieldResultWithPagination(
        "net_sales",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(netSales = carloFullAggregate.netSales),
          gabrieleData = gabrieleEmptyAggregate.copy(netSales = gabrieleFullAggregate.netSales),
          johnData = johnEmptyAggregate.copy(netSales = johnFullAggregate.netSales),
        ),
      )

      assertFieldResultWithPagination(
        "discounts",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(discounts = carloFullAggregate.discounts),
          gabrieleData = gabrieleEmptyAggregate.copy(discounts = gabrieleFullAggregate.discounts),
          johnData = johnEmptyAggregate.copy(discounts = johnFullAggregate.discounts),
        ),
      )

      assertFieldResultWithPagination(
        "refunds",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(refunds = carloFullAggregate.refunds),
          gabrieleData = gabrieleEmptyAggregate.copy(refunds = gabrieleFullAggregate.refunds),
          johnData = johnEmptyAggregate.copy(refunds = johnFullAggregate.refunds),
        ),
      )

      assertFieldResultWithPagination(
        "taxes",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(taxes = carloFullAggregate.taxes),
          gabrieleData = gabrieleEmptyAggregate.copy(taxes = gabrieleFullAggregate.taxes),
          johnData = johnEmptyAggregate.copy(taxes = johnFullAggregate.taxes),
        ),
      )

      assertFieldResultWithPagination(
        "tips",
        totalCount,
        orderedResult(
          carloData = carloEmptyAggregate.copy(tips = carloFullAggregate.tips),
          gabrieleData = gabrieleEmptyAggregate.copy(tips = gabrieleFullAggregate.tips),
          johnData = johnEmptyAggregate.copy(tips = johnFullAggregate.tips),
        ),
      )

      assertAllFieldsResultWithPagination(
        totalCount,
        orderedResult(
          carloData = carloFullAggregate,
          gabrieleData = gabrieleFullAggregate,
          johnData = johnFullAggregate,
        ),
      )
    }

    "with custom order" should {

      val carloThenGabrieleThenJohn =
        Seq(carloResult(carloFullAggregate), gabrieleResult(gabrieleFullAggregate), johnResult(johnFullAggregate))

      assertOrderByResultWithPagination("id", totalCount, carloThenGabrieleThenJohn.sortBy(_.id.toString))

      assertOrderByResultWithPagination("first_name", totalCount, carloThenGabrieleThenJohn)

    }

    "with pagination" should {
      assertAllFieldsResultWithPagination(
        totalCount,
        Seq(carloResult(carloFullAggregate)),
        pagination = Some(Pagination(1, 1)),
      )

    }

    "filtered by id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(carloResult(carloFullAggregate)),
        extraFilters = Some(s"id[]=${employeeCarlo.id}"),
      )
    }

    "filtered by location_id" should {
      assertAllFieldsResultWithPagination(
        3,
        Seq(
          carloResult(carloFullAggregate),
          gabrieleResult(emptyAggregate),
          johnResult(emptyAggregate),
        ),
        extraFilters = Some(s"location_id=${rome.id}"),
      )

      assertAllFieldsResultWithPagination(
        3,
        Seq(
          carloResult(emptyAggregate),
          gabrieleResult(
            SalesAggregate(
              count = 1,
              costs = Some(4.5.$$$),
              discounts = Some(3.8.$$$),
              giftCardSales = Some(0.$$$),
              grossProfits = Some(-8.2.$$$),
              grossSales = Some(5.0.$$$),
              netSales = Some(-3.7.$$$),
              nonTaxable = Some(0.$$$),
              refunds = Some(0.$$$),
              taxable = Some(1.3.$$$),
              taxes = Some(3.7.$$$),
              tips = Some(5.0.$$$),
            ),
          ),
          johnResult(emptyAggregate),
        ),
        extraFilters = Some(s"location_id=${london.id}"),
      )
    }
  }
}
