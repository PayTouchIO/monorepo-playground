package io.paytouch.core.reports.resources.productsales

import io.paytouch.core.data.daos.ConfiguredTestDatabase
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.VariantOptionWithType
import io.paytouch.core.reports.entities.{ OrderItemSalesAggregate, ProductSales }
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.FutureHelpers

trait ProductSalesFSpecFixtures extends OrdersFSpecFixtures with ConfiguredTestDatabase with FutureHelpers {
  val deletedProduct = Factory
    .simpleProduct(merchant, name = Some("BUBBA"), locations = locations, deletedAt = Some(now.minusYears(1)))
    .create

  val template = Factory.templateProduct(merchant, name = Some("Template Product"), locations = locations).create

  val variantOptionType = Factory.variantOptionType(template).create
  val variantOption1 = Factory.variantOption(template, variantOptionType, "Option 1").create
  val variantOption2 = Factory.variantOption(template, variantOptionType, "Option 2").create

  val variantOption1Entity =
    VariantOptionWithType(
      id = variantOption1.id,
      name = variantOption1.name,
      typeName = variantOptionType.name,
      position = variantOption1.position,
      typePosition = variantOptionType.position,
    )
  val variantOption2Entity =
    VariantOptionWithType(
      id = variantOption2.id,
      name = variantOption2.name,
      typeName = variantOptionType.name,
      position = variantOption2.position,
      typePosition = variantOptionType.position,
    )

  val variantProductA =
    Factory
      .variantProduct(
        merchant,
        template,
        name = Some("Variant Product A"),
        sku = Some("variant-A-sku"),
        upc = Some("variant-A-upc"),
        locations = locations,
      )
      .create
  Factory.productVariantOption(variantProductA, variantOption1).create
  val variantProductB =
    Factory
      .variantProduct(
        merchant,
        template,
        name = Some("Variant Product B"),
        sku = Some("variant-B-sku"),
        upc = Some("variant-B-upc"),
        locations = locations,
      )
      .create
  Factory.productVariantOption(variantProductB, variantOption2).create

  val emptyProduct1Aggregate = OrderItemSalesAggregate(count = 5)
  val fullProduct1Aggregate = emptyProduct1Aggregate.copy(
    discounts = Some(7.2.$$$),
    grossProfits = Some(-1.2.$$$),
    grossSales = Some(16.00.$$$),
    margin = Some(-10.00),
    netSales = Some(12.00.$$$),
    quantity = Some(13.50),
    returnedAmount = Some(41.00.$$$),
    returnedQuantity = Some(4.00),
    cost = Some(13.20.$$$),
    taxable = Some(11.0.$$$),
    nonTaxable = Some(1.$$$),
    taxes = Some(4.$$$),
  )

  val emptyProduct2Aggregate = OrderItemSalesAggregate(count = 1)
  val fullProduct2Aggregate = emptyProduct2Aggregate.copy(
    discounts = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    margin = Some(0),
    netSales = Some(0.$$$),
    quantity = Some(0),
    returnedAmount = Some(1.00.$$$),
    returnedQuantity = Some(1.00),
    cost = Some(0.00.$$$),
    taxable = Some(0.00.$$$),
    nonTaxable = Some(0.00.$$$),
    taxes = Some(0.$$$),
  )

  val emptyProduct3Aggregate = OrderItemSalesAggregate(count = 1)
  val fullProduct3Aggregate = emptyProduct3Aggregate.copy(
    discounts = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    margin = Some(0),
    netSales = Some(0.$$$),
    quantity = Some(0),
    returnedQuantity = Some(0),
    returnedAmount = Some(0.$$$),
    cost = Some(0.00.$$$),
    taxable = Some(0.00.$$$),
    nonTaxable = Some(0.00.$$$),
    taxes = Some(0.$$$),
  )

  val emptyVariantAAggregate = OrderItemSalesAggregate(count = 0)
  val fullVariantAAggregate = emptyVariantAAggregate.copy(
    discounts = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    margin = Some(0),
    netSales = Some(0.$$$),
    quantity = Some(0),
    returnedQuantity = Some(0),
    returnedAmount = Some(0.$$$),
    cost = Some(0.00.$$$),
    taxable = Some(0.00.$$$),
    nonTaxable = Some(0.00.$$$),
    taxes = Some(0.$$$),
  )

  val emptyVariantBAggregate = OrderItemSalesAggregate(count = 0)
  val fullVariantBAggregate = emptyVariantBAggregate.copy(
    discounts = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    margin = Some(0),
    netSales = Some(0.$$$),
    quantity = Some(0),
    returnedQuantity = Some(0),
    returnedAmount = Some(0.$$$),
    cost = Some(0.00.$$$),
    taxable = Some(0.00.$$$),
    nonTaxable = Some(0.00.$$$),
    taxes = Some(0.$$$),
  )

  val resultOrdered123AB = Seq(
    ProductSales(simpleProduct1, fullProduct1Aggregate),
    ProductSales(variantProduct2, fullProduct2Aggregate),
    ProductSales(variantProduct3, fullProduct3Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), fullVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), fullVariantBAggregate),
  )

  val resultOrdered213AB = Seq(
    ProductSales(variantProduct2, fullProduct2Aggregate),
    ProductSales(simpleProduct1, fullProduct1Aggregate),
    ProductSales(variantProduct3, fullProduct3Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), fullVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), fullVariantBAggregate),
  )

  val resultOrdered312AB = Seq(
    ProductSales(variantProduct3, fullProduct3Aggregate),
    ProductSales(simpleProduct1, fullProduct1Aggregate),
    ProductSales(variantProduct2, fullProduct2Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), fullVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), fullVariantBAggregate),
  )

  val resultOrdered321AB = Seq(
    ProductSales(variantProduct3, fullProduct3Aggregate),
    ProductSales(variantProduct2, fullProduct2Aggregate),
    ProductSales(simpleProduct1, fullProduct1Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), fullVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), fullVariantBAggregate),
  )

  val resultOrdered12AB3 = Seq(
    ProductSales(simpleProduct1, fullProduct1Aggregate),
    ProductSales(variantProduct3, fullProduct3Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), fullVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), fullVariantBAggregate),
    ProductSales(variantProduct2, fullProduct2Aggregate),
  )

  val resultOrdered13AB2 = Seq(
    ProductSales(simpleProduct1, fullProduct1Aggregate),
    ProductSales(variantProduct2, fullProduct2Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), fullVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), fullVariantBAggregate),
    ProductSales(variantProduct3, fullProduct3Aggregate),
  )

  val emptyResultOrdered123AB = Seq(
    ProductSales(simpleProduct1, emptyProduct1Aggregate),
    ProductSales(variantProduct2, emptyProduct2Aggregate),
    ProductSales(variantProduct3, emptyProduct3Aggregate),
    ProductSales(variantProductA, Seq(variantOption1Entity), emptyVariantAAggregate),
    ProductSales(variantProductB, Seq(variantOption2Entity), emptyVariantBAggregate),
  )

  val totalCount = 5

  new AdminReportService(db).triggerUpdateReports(filters = adminReportFilters).await
}
