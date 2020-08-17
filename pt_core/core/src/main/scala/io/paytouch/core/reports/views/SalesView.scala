package io.paytouch.core.reports.views

import java.util.UUID

import scala.concurrent._

import slick.jdbc.GetResult

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters._
import io.paytouch.core.reports.queries._

class SalesView(implicit val ec: ExecutionContext) extends ReportAggrView {
  import io.paytouch.core.data.driver.CustomPlainImplicits._

  type GroupBy = SalesGroupBy
  type Field = SalesFields
  type AggrResult = SalesAggregate

  protected val aggrCsvConverter = salesAggregateConverter

  val fieldsEnum = SalesFields
  lazy val endpoint = "sales"

  override def tableName(filters: ReportFilters) = tableNameAlias
  override lazy val tableNameAlias = "reports_orders"
  override lazy val idColumn = s"$tableNameAlias.merchant_id"

  def expandView(filters: ReportFilters) = None

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        data <- Option(SalesAggregate.getResultOrZero(count, r))
        key <- Option(r.nextStringOption())
      } yield ReportFields(values = data, key = key),
    )

  def locationClauses(locationIds: Seq[UUID]): Seq[String] = {
    import io.paytouch.core.data.driver.CustomPlainImplicits._
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")
  }

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.received_at_tz BETWEEN $from AND $to")

  def defaultJoins(filters: ReportFilters) = OrderView.defaultJoins(filters, withVoided = true)

  override def enrichAggrResult(
      filters: ReportAggrFilters[_],
      queryType: QueryAggrType,
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ReportFields[AggrResult]]] = {
    val withTenderTypes = filters.fields.exists(_.entryName == LocationSalesFields.TenderTypes.entryName)
    enrichResult(filters, queryType)(withTenderTypes, enrichSingleAggrResult)
  }

  private def enrichSingleAggrResult(
      singleData: SingleReportData[Option[ReportFields[AggrResult]]],
      totalsByTenderTypeByTime: Map[ReportTimeframe, Map[TransactionPaymentType, MonetaryAmount]],
    )(implicit
      user: UserContext,
    ): SingleReportData[Option[ReportFields[AggrResult]]] = {
    val baseTenderTypes = baseTenderTypeMap
    val timeframe = singleData.timeframe
    val totalsByTenderType = totalsByTenderTypeByTime.getOrElse(timeframe, Map.empty)
    val newSingleResult = singleData.result.map { singleResult =>
      val values = singleResult.values
      val fullTotalsByTenderType = baseTenderTypes ++ totalsByTenderType
      singleResult.copy(values = values.copy(tenderTypes = Some(fullTotalsByTenderType)))
    }
    singleData.copy(result = newSingleResult)
  }

  private def enrichResult[T](
      filters: ReportFilters,
      queryType: QueryAggrType,
    )(
      withTenderTypes: Boolean,
      f: (SingleReportData[Option[T]],
          Map[ReportTimeframe, Map[TransactionPaymentType, MonetaryAmount]]) => SingleReportData[Option[T]],
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[T]] =
    EnrichResult { data =>
      if (withTenderTypes)
        getTenderTypesExpansions(filters, queryType).map { totalsByTenderType =>
          data.map(result => f(result, totalsByTenderType))
        }
      else Future.successful(data)
    }

  private def getTenderTypesExpansions(
      filters: ReportFilters,
      queryType: QueryAggrType,
    )(implicit
      db: Database,
      user: UserContext,
    ): Future[Map[ReportTimeframe, Map[TransactionPaymentType, MonetaryAmount]]] = {

    implicit val getResult = GetResult(r =>
      (
        ReportTimeframe(r.nextString(), r.nextString()),
        TransactionPaymentType.withName(r.nextString()),
        MonetaryAmount(r.nextBigDecimal()),
      ),
    )

    val aggrFunction = queryType match {
      case AverageQuery => "AVG"
      case _            => "SUM"
    }

    // TODO - clean up and improve performance!
    val query =
      s"""WITH $intervalSeries AS (${intervalsView(filters)})
          SELECT $intervalStartTimeSelector,
                  $intervalEndTimeSelector,
                  pt.payment_type,
                  $aggrFunction((
                    CASE pt.type IN (${TransactionType.isPositive.map(_.entryName).asInParametersList})
                      WHEN TRUE THEN COALESCE((payment_details::json->>'amount')::float, 0)
                      WHEN FALSE THEN COALESCE((payment_details::json->>'amount')::float * -1, 0)
                    END
                  ) - COALESCE(total_fees, 0)) AS total
          FROM $intervalSeries
          JOIN ${tableName(filters)} AS $tableNameAlias
          ON    $tableNameAlias.received_at_tz BETWEEN $intervalStartTimeSelector AND $intervalEndTimeSelector
          AND   $tableNameAlias.location_id IN (${filters.locationIds.asInParametersList})
          AND   ${OrderView.defaultClauses(filters, withVoided = true).mkString(" AND ")}
          JOIN payment_transactions pt
          ON    $tableNameAlias.id = pt.order_id
          LEFT JOIN LATERAL (
            SELECT SUM(ptf.amount) as total_fees
            FROM payment_transaction_fees ptf
            WHERE ptf.payment_transaction_id = pt.id
          ) fees
          ON true
          WHERE pt.payment_details->'transactionResult'::text = 'null'
          OR    pt.payment_details->>'transactionResult'::text != '${CardTransactionResultType.Declined.entryName}'
          GROUP BY $intervalStartTimeSelector,
                   $intervalEndTimeSelector,
                   pt.payment_type""".stripMargin

    db.run[Seq[(ReportTimeframe, TransactionPaymentType, MonetaryAmount)]](
      sql"#$query".as[(ReportTimeframe, TransactionPaymentType, MonetaryAmount)],
    ).map { result =>
      result.groupBy { case (time, _, _) => time }.transform {
        case (_, byTimeValues) =>
          byTimeValues.groupBy { case (_, paymentType, _) => paymentType }.transform { (_, byPaymentTypeValues) =>
            MonetaryAmount.sum(byPaymentTypeValues.map { case (_, _, amount) => amount })
          }
      }
    }
  }

  private def baseTenderTypeMap(implicit user: UserContext): Map[TransactionPaymentType, MonetaryAmount] =
    TransactionPaymentType.reportValues.map(_ -> MonetaryAmount(0, user.currency)).toMap
}

object SalesView {
  def apply()(implicit ec: ExecutionContext): SalesView =
    new SalesView()
}
