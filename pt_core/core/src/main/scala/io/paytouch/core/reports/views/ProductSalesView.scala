package io.paytouch.core.reports.views

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.entities.{ UserContext, VariantOptionWithType }
import io.paytouch.core.reports.async.exporters.CSVConverterHelper
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ OrderItemSalesAggregate, ProductSales, ReportFields, SingleReportData }
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportFilters, ReportListFilters }
import io.paytouch.core.reports.queries._
import io.paytouch.core.services.VariantService
import io.paytouch.core.utils.Formatters
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

class ProductSalesView(val variantService: VariantService)(implicit val ec: ExecutionContext)
    extends ReportListView
       with ReportAggrView
       with ExtendedView[ProductSalesView] {
  import io.paytouch.core.data.driver.CustomPlainImplicits._

  type GroupBy = ProductSalesGroupBy
  type Field = ProductSalesFields
  type AggrResult = ProductSales

  type OrderBy = ProductSalesOrderByFields
  type ListResult = ProductSales

  override val mandatoryIdsFilter = true

  def listResultConverter: CSVConverterHelper[ProductSales] = productSalesConverter

  protected def aggrCsvConverter = productSalesConverter

  val fieldsEnum = ProductSalesFields
  val orderByEnum = ProductSalesOrderByFields
  val endpoint = "product_sales"

  override def tableName(filters: ReportFilters) =
    s"reports_product_sales_func('${filters.merchantId}', '{${filters.locationIds.mkString(",")}}', '${filters.from}', '${filters.to}')"
  override lazy val tableNameAlias: String = "reports_product_sales"

  val listTable = "products"

  override lazy val idColumn = s"products.id"

  def listWhereClauses(filters: ReportFilters) = commonWhereClauses(filters)

  def aggrResult(implicit user: UserContext): GetResult[Option[ReportFields[AggrResult]]] =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        values <- listResult.apply(r)
        key <- Option(r.nextStringOption())
      } yield ReportFields(
        values = values,
        key = Some(values.id.toString),
      ),
    )

  override def enrichAggrResult(
      filters: ReportAggrFilters[_],
      queryType: QueryAggrType,
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ReportFields[AggrResult]]] =
    EnrichResult { data =>
      val variantIds = data.flatMap(_.result.map(_.values.id))
      val variantOptionsPerProductR = variantService.findVariantOptionsByVariantIds(variantIds)
      for {
        variantOptionsPerProduct <- variantOptionsPerProductR
      } yield data.map(result => enrichSingleAggrResult(result, variantOptionsPerProduct))
    }

  private def enrichSingleAggrResult(
      singleData: SingleReportData[Option[ReportFields[AggrResult]]],
      variantOptions: Map[UUID, Seq[VariantOptionWithType]],
    ): SingleReportData[Option[ReportFields[AggrResult]]] = {
    val newReportFields = singleData.result.map { res =>
      val item = res.values
      val newItem = item.copy(options = variantOptions.get(item.id))
      res.copy(values = newItem)
    }
    singleData.copy(result = newReportFields)
  }

  override protected def enrichListResult(
      filters: ReportListFilters[_],
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ListResult]] =
    EnrichResult { data =>
      val variantIds = data.flatMap(_.result.map(_.id))
      val variantOptionsPerProductR = variantService.findVariantOptionsByVariantIds(variantIds)
      for {
        variantOptionsPerProduct <- variantOptionsPerProductR
      } yield data.map(result => enrichSingleListResult(result, variantOptionsPerProduct))
    }

  private def enrichSingleListResult(
      singleData: SingleReportData[Option[ListResult]],
      variantOptions: Map[UUID, Seq[VariantOptionWithType]],
    ): SingleReportData[Option[ListResult]] = {
    val newSingleDataResult = singleData.result.map(res => res.copy(options = variantOptions.get(res.id)))
    singleData.copy(result = newSingleDataResult)
  }

  protected def listResult(implicit user: UserContext): GetResult[Option[ProductSales]] =
    GetResult { r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        deletedAt <- Option(r.nextStringOption().map(ZonedDateTime.parse(_, date2TzDateTimeFormatter)))
        id <- Option(r.nextUUID())
        name <- Option(r.nextString())
        sku <- Option(r.nextStringOption())
        upc <- Option(r.nextStringOption())
        data <- Option(OrderItemSalesAggregate.getResultOrZero(count, r))
      } yield ProductSales(
        id = id,
        name = name,
        sku = sku,
        upc = upc,
        deletedAt = deletedAt,
        options = None,
        data = data,
      )
    }

  def locationClauses(locationIds: Seq[UUID]): Seq[String] =
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.received_at_tz BETWEEN $from AND $to")

  def defaultJoins(filters: ReportFilters) = {
    val joinClauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      locationClauses(filters.locationIds) ++
      Seq(s"products.id = $tableNameAlias.product_id")

    val whereClauses = commonWhereClauses(filters)

    Seq(s"CROSS JOIN products") ++
      Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${joinClauses.mkString(" AND ")}") ++
      Seq(s"WHERE ${whereClauses.mkString(" AND ")}")
  }

  def commonWhereClauses(filters: ReportFilters) = {
    val idClauses: Seq[String] =
      filters.ids.fold(Seq.empty[String])(ids => Seq(s"$listTable.id IN (${ids.asInParametersList})"))
    val categoryIdClauses: Seq[String] =
      filters.categoryIds.fold(Seq.empty[String]) { categoryIds =>
        Seq(
          s"$listTable.id IN (SELECT product_id FROM product_categories pc WHERE pc.category_id IN (${categoryIds.asInParametersList}))",
        )
      }
    Seq(
      s"$listTable.merchant_id = '${filters.merchantId}'",
      s"$listTable.scope = 'product'",
      s"$listTable.type IN ('simple', 'variant')",
      s"($listTable.deleted_at IS NULL OR $listTable.deleted_at > '${Formatters.LocalDateTimeFormatter.format(filters.from)}'::timestamp)",
    ) ++ idClauses ++ categoryIdClauses
  }
}

object ProductSalesView {
  def apply(variantService: VariantService)(implicit ec: ExecutionContext): ProductSalesView =
    new ProductSalesView(variantService)
}
