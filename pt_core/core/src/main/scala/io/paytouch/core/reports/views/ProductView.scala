package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.entities.{ MonetaryAmount, UserContext, VariantOptionWithType }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ ProductTop, SingleReportData }
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.reports.queries.EnrichResult
import io.paytouch.core.services.VariantService
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

class ProductView(val variantService: VariantService)(implicit val ec: ExecutionContext) extends ReportTopView {

  type OrderBy = ProductOrderByFields
  type TopResult = ProductTop

  val topCSVConverter = productTopConverter

  val orderByEnum = ProductOrderByFields

  val endpoint = "products"
  override lazy val tableNameAlias = "order_items"

  override def enrichTopResult(implicit user: UserContext): EnrichResult[Option[TopResult]] =
    EnrichResult { data =>
      val variantIds = data.flatMap(_.result.map(_.id))
      val variantOptionsPerProductR = variantService.findVariantOptionsByVariantIds(variantIds)
      for {
        variantOptionsPerProduct <- variantOptionsPerProductR
      } yield data.map(result => enrichSingleTopResult(result, variantOptionsPerProduct))
    }

  private def enrichSingleTopResult(
      singleData: SingleReportData[Option[TopResult]],
      variantOptions: Map[UUID, Seq[VariantOptionWithType]],
    ): SingleReportData[Option[TopResult]] = {
    val newSingleDataResult = singleData.result.map(res => res.copy(options = variantOptions.get(res.id)))
    singleData.copy(result = newSingleDataResult)
  }

  def locationClauses(locationIds: Seq[UUID]): Seq[String] = Seq.empty

  def dateClauses(from: String, to: String): Seq[String] = Seq.empty

  def topResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        id <- r.nextStringOption().map(UUID.fromString)
        margin <- r.nextBigDecimalOption()
        name <- r.nextStringOption()
        netSales <- r.nextBigDecimalOption().map(a => MonetaryAmount(a))
        profit <- r.nextBigDecimalOption().map(a => MonetaryAmount(a))
        quantitySold <- r.nextBigDecimalOption()
        revenue <- r.nextBigDecimalOption().map(a => MonetaryAmount(a))
      } yield ProductTop(
        id = id,
        name = name,
        netSales = netSales,
        profit = profit,
        quantitySold = quantitySold,
        revenue = revenue,
        margin = margin,
        options = None,
      ),
    )

  def defaultJoins(filters: ReportFilters): Seq[String] =
    OrderView.defaultJoins(filters) ++
      Seq(
        s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON reports_orders.id = $tableNameAlias.order_id",
        s"LEFT OUTER JOIN products ON products.id = $tableNameAlias.product_id",
      )

}

object ProductView {
  def apply(variantService: VariantService)(implicit ec: ExecutionContext): ProductView =
    new ProductView(variantService)
}
