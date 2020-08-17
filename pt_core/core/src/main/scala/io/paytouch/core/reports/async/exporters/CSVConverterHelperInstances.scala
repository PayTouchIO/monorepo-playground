package io.paytouch.core.reports.async.exporters

import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.entities.VariantOptionWithType
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums.{ SalesFields, _ }
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.utils.{ EnumEntrySnake, Formatters }

case class Column[T](
    field: EnumEntrySnake,
    rowExtractor: T => String,
    headerOverride: Option[String] = None,
  ) {
  def header = headerOverride.getOrElse(toPretty(field.entryName))

  private def toPretty(text: String): String = text.split("_").map(_.capitalize).mkString(" ")
}

object CSVConverterHelperInstances {

  def converterBuilder[T](innerConverter: CSVConverterHelper[T]): ReportResponseConverter[T] = {
    val reportDataConverter = new ReportDataConverter(innerConverter)
    new ReportResponseConverter[T](reportDataConverter)
  }

  class ReportResponseConverter[T](val innerConverter: CSVConverterHelper[ReportData[T]])
      extends CSVConverterHelper[ReportResponse[T]] {

    def header(ts: Seq[ReportResponse[T]], f: ReportFilters) = innerConverter.header(ts.flatMap(_.data), f)

    def rows(t: ReportResponse[T], f: ReportFilters): List[List[String]] =
      t.data.toList.flatMap(innerConverter.rows(_, f))

  }

  class ReportFieldsConverter[T](val innerConverter: CSVConverterHelper[T])
      extends CSVConverterHelper[ReportFields[T]] {

    def header(ts: Seq[ReportFields[T]], f: ReportFilters): List[String] =
      List(f.toGroupByHeader()) ++ innerConverter.header(ts.map(_.values), f)

    def rows(t: ReportFields[T], f: ReportFilters): List[List[String]] = {
      val values: List[String] = innerConverter.rows(t.values, f).fold(List.empty)(_ ++ _)
      val key: List[String] = {
        val keyValue = if (f.toGroupByHeader().isEmpty) None else t.key
        List(keyValue)
      }
      List(key ++ values)
    }
  }

  class ReportDataConverter[T](innerConverter: CSVConverterHelper[T]) extends CSVConverterHelper[ReportData[T]] {

    def header(ts: Seq[ReportData[T]], f: ReportFilters) = {
      val dateColumns =
        if (f.interval == ReportInterval.NoInterval) List()
        else List("Start Time", "End Time")
      dateColumns ++ innerConverter.header(ts.flatMap(_.result), f)
    }

    def rows(t: ReportData[T], f: ReportFilters): List[List[String]] = {
      val timeframe: List[String] =
        if (f.interval == ReportInterval.NoInterval) List()
        else List(t.timeframe.start, t.timeframe.end)

      t.result.toList.flatMap { data =>
        val lines = innerConverter.rows(data, f)
        lines.map(l => timeframe ++ l)
      }
    }
  }

  implicit lazy val reportCountConverter: CSVConverterHelper[ReportCount] = new CSVConverterHelper[ReportCount] {

    def header(ts: Seq[ReportCount], f: ReportFilters): List[String] =
      List(f.toGroupByHeader(), "Count")

    def rows(t: ReportCount, f: ReportFilters): List[List[String]] = {
      val row: List[String] = List(t.key, t.count)
      List(row)
    }
  }

  implicit lazy val reportDataCountConverter = new ReportDataConverter[ReportCount](reportCountConverter)

  implicit lazy val customerAggregateConverter = new CSVWithOrderableColumnsConverterHelper[CustomerAggregate] {

    lazy val columns = Seq(
      column(CustomerFields.Count, _.count),
      column(CustomerFields.Spend, _.spend),
    )

  }

  implicit lazy val customerTopConverter = new CSVConverterHelper[CustomerTop] {

    def header(t: Seq[CustomerTop], f: ReportFilters): List[String] =
      List("Customer ID", "First Name", "Last Name", "Profit", "Spend", "Margin", "Visits")

    def rows(t: CustomerTop, f: ReportFilters): List[List[String]] = {
      val row: List[String] = List(t.id, t.firstName, t.lastName, t.profit, t.spend, t.margin, t.visits)
      List(row)
    }
  }

  implicit lazy val groupTopConverter = new CSVConverterHelper[GroupTop] {

    def header(t: Seq[GroupTop], f: ReportFilters): List[String] =
      List("Group ID", "Name", "Spend", "Profit", "Margin", "Visits")

    def rows(t: GroupTop, f: ReportFilters): List[List[String]] = {
      val row: List[String] = List(t.id, t.name, t.spend, t.profit, t.margin, t.visits)
      List(row)
    }
  }

  implicit lazy val giftCardPassAggregateConverter =
    new CSVWithOrderableColumnsConverterHelper[GiftCardPassAggregate] {

      lazy val columns = Seq(
        column(GiftCardPassFields.Count, _.count),
        column(GiftCardPassFields.Customers, _.customers),
        column(GiftCardPassFields.Total, _.total, Some("Total Value")),
        column(GiftCardPassFields.Redeemed, _.redeemed, Some("Total Redeemed")),
        column(GiftCardPassFields.Unused, _.unused, Some("Total Unused")),
      )

    }

  implicit lazy val orderAggregateConverter = new CSVWithOrderableColumnsConverterHelper[OrderAggregate] {

    lazy val columns = Seq(
      column(OrderFields.Count, _.count),
      column(OrderFields.Profit, _.profit),
      column(OrderFields.Revenue, _.revenue),
      column(OrderFields.WaitingTime, _.waitingTimeInSeconds, Some("Waiting Time in Seconds")),
    )

  }

  implicit lazy val orderTaxRateAggregateConverter =
    new CSVWithOrderableColumnsConverterHelper[OrderTaxRateAggregate] {

      lazy val columns = Seq(
        column(OrderTaxRateFields.Count, _.count),
        column(OrderTaxRateFields.Amount, _.amount, Some("Tax Amount")),
      )

    }

  implicit lazy val salesAggregateConverter = new CSVWithOrderableColumnsConverterHelper[SalesAggregate] {

    lazy val columns = Seq(
      column(SalesFields.Count, _.count, Some("Number of Sales")),
      column(SalesFields.Costs, _.costs, Some("COGS")),
      column(SalesFields.Discounts, _.discounts),
      column(SalesFields.GiftCardSales, _.giftCardSales, Some("Gift Card Sales")),
      column(SalesFields.GrossProfits, _.grossProfits, Some("Profits")),
      column(SalesFields.GrossSales, _.grossSales),
      column(SalesFields.NetSales, _.netSales),
      column(SalesFields.NonTaxable, _.nonTaxable, Some("Non-Taxable Sales")),
      column(SalesFields.Refunds, _.refunds),
      column(SalesFields.Taxable, _.taxable, Some("Taxable Sales")),
      column(SalesFields.Taxes, _.taxes, Some("Tax Collected")),
      column(SalesFields.Tips, _.tips),
    ) ++ TransactionPaymentType.reportValues.map { pt =>
      column(
        SalesFields.TenderTypes,
        t => t.tenderTypes.flatMap(_.get(pt).map(fromMonetaryToString)).getOrElse("0.00"),
        Some(s"Tender - ${pt.entryName.replace("_", " ")}"),
      )
    }

  }

  implicit lazy val locationGiftCardPassesConverter =
    new CSVWithOrderableColumnsConverterHelper[LocationGiftCardPasses] {

      lazy val columns = Seq(
        column(LocationGiftCardPassFields.Id, _.id, Some("Location ID")),
        column(
          LocationGiftCardPassFields.Name,
          ls => s"${ls.name} - ${ls.addressLine1.getOrElse("")}",
          Some("Location Name"),
        ),
      )

      override def header(ts: Seq[Data], f: ReportFilters) =
        super.header(ts, f) ++ giftCardPassAggregateConverter.header(ts.map(_.data), f)

      override def rows(t: Data, f: ReportFilters) =
        List(super.rows(t, f).head ++ giftCardPassAggregateConverter.rows(t.data, f).head)

    }

  implicit lazy val locationSalesConverter = new CSVWithOrderableColumnsConverterHelper[LocationSales] {

    lazy val columns = Seq(
      column(LocationSalesFields.Id, _.id, Some("Location ID")),
      column(LocationSalesFields.Name, ls => s"${ls.name} - ${ls.addressLine1.getOrElse("")}", Some("Location Name")),
    )

    override def header(ts: Seq[Data], f: ReportFilters) =
      super.header(ts, f) ++ salesAggregateConverter.header(ts.map(_.data), f)

    override def rows(t: Data, f: ReportFilters) =
      List(super.rows(t, f).head ++ salesAggregateConverter.rows(t.data, f).head)

  }

  implicit lazy val employeeSalesConverter = new CSVWithOrderableColumnsConverterHelper[EmployeeSales] {

    lazy val columns = Seq(
      column(EmployeeSalesFields.Id, _.id, Some("Employee ID")),
      column(EmployeeSalesFields.FirstName, _.firstName),
      column(EmployeeSalesFields.LastName, _.lastName),
    )

    override def header(ts: Seq[Data], f: ReportFilters) =
      super.header(ts, f) ++ salesAggregateConverter.header(ts.map(_.data), f)

    override def rows(t: Data, f: ReportFilters) =
      List(super.rows(t, f).head ++ salesAggregateConverter.rows(t.data, f).head)

  }

  implicit lazy val productTopConverter = new CSVConverterHelper[ProductTop] {

    def header(ts: Seq[ProductTop], f: ReportFilters): List[String] = {
      val variantOptionHeader = VariantOptionWithType.headers(ts.flatMap(_.options))
      List(
        "Product ID",
        "Product Name",
        "Quantity Sold",
        "Revenue",
        "Net Sales",
        "Profit",
        "Margin",
      ) ++ variantOptionHeader
    }

    def rows(t: ProductTop, f: ReportFilters): List[List[String]] = {
      val variantOptionRows: List[String] = VariantOptionWithType.rows(t.options.getOrElse(Seq.empty))
      val row: List[String] =
        List(t.id, t.name, t.quantitySold, t.revenue, t.netSales, t.profit, t.margin)
      List(row ++ variantOptionRows)
    }
  }

  implicit lazy val orderItemSalesAggregateConverter: CSVWithOrderableColumnsConverterHelper[OrderItemSalesAggregate] =
    new CSVWithOrderableColumnsConverterHelper[OrderItemSalesAggregate] {
      lazy val columns = Seq(
        column(OrderItemSalesFields.Count, _.count),
        column(OrderItemSalesFields.Discounts, _.discounts),
        column(OrderItemSalesFields.GrossProfits, _.grossProfits, Some("Gross Profits")),
        column(OrderItemSalesFields.NetSales, _.netSales, Some("Net Sales")),
        column(OrderItemSalesFields.GrossSales, _.grossSales, Some("Gross Sales")),
        column(OrderItemSalesFields.Margin, _.margin, Some("Margin")),
        column(OrderItemSalesFields.Quantity, _.quantity, Some("Quantity Sold")),
        column(OrderItemSalesFields.ReturnedAmount, _.returnedAmount),
        column(OrderItemSalesFields.ReturnedQuantity, _.returnedQuantity),
        column(OrderItemSalesFields.Cost, _.cost, Some("COGS")),
        column(OrderItemSalesFields.Taxable, _.taxable, Some("Taxable Sales")),
        column(OrderItemSalesFields.NonTaxable, _.nonTaxable, Some("Non-Taxable Sales")),
        column(OrderItemSalesFields.Taxes, _.taxes, Some("Tax Collected")),
      )
    }

  implicit lazy val productSalesConverter = new CSVWithOrderableColumnsConverterHelper[ProductSales] {

    lazy val columns = Seq(
      column(ProductSalesFields.Id, _.id, Some("Product ID")),
      column(
        ProductSalesFields.Name,
        { productSales =>
          val variantOptionRows = productSales.options.getOrElse(Seq.empty).map(_.name)
          (Seq(productSales.name) ++ variantOptionRows).mkString(" - ")
        },
        Some("Product Name"),
      ),
      column(ProductSalesFields.Sku, _.sku, Some("SKU")),
      column(ProductSalesFields.Upc, _.upc, Some("UPC")),
      column(
        ProductSalesFields.DeletedAt,
        _.deletedAt.map(d => Formatters.ZonedDateTimeFormatter.format(d)),
        Some("Deleted at"),
      ),
    )

    override def header(ts: Seq[Data], f: ReportFilters) =
      super.header(ts, f) ++ orderItemSalesAggregateConverter.header(ts.map(_.data), f)

    override def rows(t: Data, f: ReportFilters) =
      List(super.rows(t, f).head ++ orderItemSalesAggregateConverter.rows(t.data, f).head)
  }

  implicit lazy val categorySalesConverter = new CSVWithOrderableColumnsConverterHelper[CategorySales] {

    lazy val columns = Seq(
      column(CategorySalesOrderByFields.Id, _.id, Some("Category ID")),
      column(CategorySalesOrderByFields.Name, _.name, Some("Category Name")),
    )

    override def header(ts: Seq[Data], f: ReportFilters) =
      super.header(ts, f) ++ orderItemSalesAggregateConverter.header(ts.map(_.data), f)

    override def rows(t: Data, f: ReportFilters) =
      List(super.rows(t, f).head ++ orderItemSalesAggregateConverter.rows(t.data, f).head)

  }

  implicit lazy val loyaltyOrdersAggregateConverter =
    new CSVWithOrderableColumnsConverterHelper[LoyaltyOrdersAggregate] {

      lazy val columns = Seq(
        column(LoyaltyOrdersFields.Count, _.count),
        column(LoyaltyOrdersFields.Amount, _.amount, Some("Amount")),
      )

    }

  implicit lazy val rewardRedemptionsAggregateConverter =
    new CSVWithOrderableColumnsConverterHelper[RewardRedemptionsAggregate] {

      lazy val columns = Seq(
        column(RewardRedemptionsFields.Count, _.count),
        column(RewardRedemptionsFields.Value, _.value, Some("Value")),
      )

    }

}
