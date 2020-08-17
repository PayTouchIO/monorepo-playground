package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.VariantOptionWithType
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportTopFilters
import io.paytouch.core.reports.views._

class ProductTopCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[ProductTop]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val productId1 = UUID.randomUUID
      val productId2 = UUID.randomUUID

      val variantOptionWithTypes1 = Seq(
        VariantOptionWithType(UUID.randomUUID, "my option A", "my option type A", position = 1, typePosition = 1),
        VariantOptionWithType(UUID.randomUUID, "my option B", "my option type B", position = 2, typePosition = 2),
      )

      val variantOptionWithTypes2 =
        Seq(VariantOptionWithType(UUID.randomUUID, "my option A", "my option type C", position = 1, typePosition = 3))

      val entity: ReportResponse[ProductTop] = reportResponseBuilder(
        Seq(
          ProductTop(productId1, "my product", 43.43, 2.5.$$$, 5.$$$, 10.$$$, 32.2, Some(variantOptionWithTypes1)),
          ProductTop(
            productId2,
            "my other product",
            12.12,
            1.5.$$$,
            3.$$$,
            24.$$$,
            12.5,
            Some(variantOptionWithTypes2),
          ),
        ),
      )

      implicit val converterHelper = converterBuilder(productTopConverter)
      val filters = ReportTopFilters[ProductView](
        ProductView(mockVariantService),
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        ProductOrderByFields.values,
        5,
        UUID.randomUUID,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List(
        "Start Time",
        "End Time",
        "Product ID",
        "Product Name",
        "Quantity Sold",
        "Revenue",
        "Net Sales",
        "Profit",
        "Margin",
        "Variant Option 1",
        "Variant Option Type 1",
        "Variant Option 2",
        "Variant Option Type 2",
      )
      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
        productId1.toString,
        "my product",
        "43.43",
        "5.00",
        "2.50",
        "10.00",
        "32.20",
        "my option A",
        "my option type A",
        "my option B",
        "my option type B",
      )
      rows(1) ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
        productId2.toString,
        "my other product",
        "12.12",
        "3.00",
        "1.50",
        "24.00",
        "12.50",
        "my option A",
        "my option type C",
        "",
        "",
      )

    }

  }

}
