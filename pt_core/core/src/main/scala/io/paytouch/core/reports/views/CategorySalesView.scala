package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelper
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ CategorySales, OrderItemSalesAggregate }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object CategorySalesView extends CategorySalesView
trait CategorySalesView extends ReportListView {
  import io.paytouch.core.data.driver.CustomPlainImplicits._

  type OrderBy = CategorySalesOrderByFields
  type ListResult = CategorySales

  def listResultConverter: CSVConverterHelper[CategorySales] = categorySalesConverter

  val orderByEnum = CategorySalesOrderByFields
  val endpoint = "category_sales"

  override def tableName(filters: ReportFilters) =
    s"reports_product_sales_func('${filters.merchantId}', '{${filters.locationIds.mkString(",")}}', '${filters.from}', '${filters.to}')"
  override lazy val tableNameAlias: String = "reports_product_sales"

  override lazy val idColumn = "categories.id"

  val listTable = "categories"

  def listWhereClauses(filters: ReportFilters) = commonWhereClauses(filters)

  protected def listResult(implicit user: UserContext): GetResult[Option[CategorySales]] =
    GetResult { r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        id <- Option(r.nextUUID())
        name <- Option(r.nextString())
        data <- Option(OrderItemSalesAggregate.getResultOrZero(count, r))
      } yield CategorySales(
        id = id,
        name = name,
        data = data,
      )
    }

  def locationClauses(locationIds: Seq[UUID]): Seq[String] =
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.received_at_tz BETWEEN $from AND $to")

  def defaultJoins(filters: ReportFilters) = {
    val joinClauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      locationClauses(filters.locationIds) ++
      Seq(s"pc.product_id = $tableNameAlias.is_variant_of_product_id")

    val whereClauses = commonWhereClauses(filters)

    Seq(s"CROSS JOIN categories") ++
      Seq("LEFT JOIN product_categories pc ON categories.id = pc.category_id ") ++
      Seq(s"LEFT JOIN ${tableName(filters)} AS $tableNameAlias ON ${joinClauses.mkString(" AND ")}") ++
      Seq(s"WHERE ${whereClauses.mkString(" AND ")}")
  }

  private def commonWhereClauses(filters: ReportFilters) = {
    import cats.implicits._
    val allIds = filters.ids.map(_.toList) |+| filters.categoryIds.map(_.toList)
    Seq(
      s"categories.merchant_id = '${filters.merchantId}'",
      "categories.parent_category_id IS NULL",
      s"categories.id IN (SELECT category_id FROM category_locations cl WHERE cl.location_id IN (${filters.locationIds.asInParametersList}))",
    ) ++ allIds.fold(Seq.empty[String])(ids => Seq(s"categories.id IN (${ids.asInParametersList})"))
  }
}
