package io.paytouch.core.reports.views

import java.util.UUID

import scala.concurrent._

import com.typesafe.scalalogging.LazyLogging

import slick.jdbc.GetResult

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportFilters, ReportListFilters }
import io.paytouch.core.reports.queries._

class LocationSalesView(implicit val ec: ExecutionContext)
    extends ReportListView
       with ReportAggrView
       with ExtendedView[LocationSalesView]
       with LazyLogging {
  import io.paytouch.core.data.driver.CustomPlainImplicits._

  type GroupBy = NoGroupBy
  type Field = LocationSalesFields
  type AggrResult = LocationSales

  type OrderBy = LocationSalesOrderByFields
  type ListResult = AggrResult

  val listResultConverter = locationSalesConverter
  protected val aggrCsvConverter = locationSalesConverter

  val orderByEnum = LocationSalesOrderByFields
  val fieldsEnum = LocationSalesFields
  val endpoint = "location_sales"

  override def tableName(filters: ReportFilters) = OrderView.tableName(filters)
  override lazy val tableNameAlias: String = OrderView.tableNameAlias

  override lazy val idColumn = s"locations.id"

  val listTable = "locations"

  def listWhereClauses(filters: ReportFilters) = {
    val userAccessibleLocationIds = filters.locationIds
    val filterByLocationIds = filters.ids.getOrElse(userAccessibleLocationIds)
    Seq(s"locations.id IN (${filterByLocationIds.asInParametersList})")
  }

  def aggrResult(implicit user: UserContext): GetResult[Option[ReportFields[LocationSales]]] =
    GetResult(r =>
      for {
        values <- listResult.apply(r)
      } yield ReportFields(values = values, key = Some(values.id.toString)),
    )

  protected def listResult(implicit user: UserContext): GetResult[Option[LocationSales]] =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        addressLine1 <- Option(r.nextStringOption())
        id <- Option(r.nextUUID())
        name <- r.nextStringOption()
        data <- Option(SalesAggregate.getResultOrZero(count, r))
      } yield LocationSales(id = id, name = name, addressLine1 = addressLine1, data = data),
    )

  def locationClauses(locationIds: Seq[UUID]): Seq[String] =
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = OrderView.dateClauses(from, to)

  def defaultJoins(filters: ReportFilters) = {
    val onClauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      Seq(s"locations.id = $tableNameAlias.location_id") ++ locationClauses(filters.locationIds) ++ OrderView
      .defaultClauses(filters, withVoided = true)

    val whereClauses = listWhereClauses(filters)
    Seq("CROSS JOIN locations") ++
      Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${onClauses.mkString(" AND ")}") ++
      Seq(s"WHERE ${whereClauses.mkString(" AND ")}")
  }

  override def enrichAggrResult(
      filters: ReportAggrFilters[_],
      queryType: QueryAggrType,
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ReportFields[AggrResult]]] = {
    val withTenderTypes = filters.fields.exists(_.entryName == LocationSalesFields.TenderTypes.entryName)
    enrichResult(filters)(withTenderTypes, enrichSingleAggrResult)
  }

  private def enrichSingleAggrResult(
      singleData: SingleReportData[Option[ReportFields[AggrResult]]],
      totalsByTenderTypeByLocationByTime: Map[ReportTimeframe, Map[UUID, Map[TransactionPaymentType, MonetaryAmount]]],
    )(implicit
      user: UserContext,
    ): SingleReportData[Option[ReportFields[AggrResult]]] = {
    val baseTenderTypes = baseTenderTypeMap
    val timeframe = singleData.timeframe
    val totalsByTenderTypeByLocation = totalsByTenderTypeByLocationByTime.getOrElse(timeframe, Map.empty)
    val newSingleResult = singleData.result.map { singleResult =>
      val values = singleResult.values
      val totalsByTenderType = baseTenderTypes ++ totalsByTenderTypeByLocation.getOrElse(values.id, Map.empty)
      val newData = values.data.copy(tenderTypes = Some(totalsByTenderType))
      singleResult.copy(values = values.copy(data = newData))
    }
    singleData.copy(result = newSingleResult)
  }

  override protected def enrichListResult(
      filters: ReportListFilters[_],
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[ListResult]] = {
    val withTenderTypes = filters.fields.exists(_.entryName == LocationSalesOrderByFields.TenderTypes.entryName)
    enrichResult(filters)(withTenderTypes, enrichSingleListResult)
  }

  private def enrichSingleListResult(
      singleData: SingleReportData[Option[ListResult]],
      totalsByTenderTypeByLocationByTime: Map[ReportTimeframe, Map[UUID, Map[TransactionPaymentType, MonetaryAmount]]],
    )(implicit
      user: UserContext,
    ): SingleReportData[Option[ListResult]] = {
    val baseTenderTypes = baseTenderTypeMap
    val timeframe = singleData.timeframe
    val totalsByTenderTypeByLocation = totalsByTenderTypeByLocationByTime.getOrElse(timeframe, Map.empty)
    val newSingleResult = singleData.result.map { singleResult =>
      val totalsByTenderType = baseTenderTypes ++ totalsByTenderTypeByLocation.getOrElse(singleResult.id, Map.empty)
      singleResult.copy(data = singleResult.data.copy(tenderTypes = Some(totalsByTenderType)))
    }
    singleData.copy(result = newSingleResult)
  }

  private def enrichResult[T](
      filters: ReportFilters,
    )(
      withTenderTypes: Boolean,
      f: (SingleReportData[Option[T]],
          Map[ReportTimeframe, Map[UUID, Map[TransactionPaymentType, MonetaryAmount]]]) => SingleReportData[Option[T]],
    )(implicit
      db: Database,
      user: UserContext,
    ): EnrichResult[Option[T]] =
    EnrichResult { data =>
      if (withTenderTypes)
        getTenderTypesExpansions(filters).map { totalsByTenderTypeByLocation =>
          data.map(result => f(result, totalsByTenderTypeByLocation))
        }
      else Future.successful(data)
    }

  private def getTenderTypesExpansions(
      filters: ReportFilters,
    )(implicit
      db: Database,
      user: UserContext,
    ): Future[Map[ReportTimeframe, Map[UUID, Map[TransactionPaymentType, MonetaryAmount]]]] = {

    implicit val getResult = GetResult(r =>
      (
        ReportTimeframe(r.nextString(), r.nextString()),
        r.nextUUID(),
        TransactionPaymentType.withName(r.nextString()),
        MonetaryAmount(r.nextBigDecimal()),
      ),
    )

    // TODO - clean up and improve performance!
    val query =
      s"""WITH $intervalSeries AS (${intervalsView(filters)})
          SELECT $intervalStartTimeSelector,
                  $intervalEndTimeSelector,
                  $tableNameAlias.location_id,
                  pt.payment_type,
                  SUM(
                    CASE pt.type IN (${TransactionType.isPositive.map(_.entryName).asInParametersList})
                      WHEN TRUE THEN COALESCE((payment_details::json->>'amount')::float, 0)
                      WHEN FALSE THEN COALESCE((payment_details::json->>'amount')::float * -1, 0)
                    END
                  ) - SUM(COALESCE(total_fees, 0)) AS total
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
                   $tableNameAlias.location_id,
                   pt.payment_type""".stripMargin

    logger.debug(s"Generated query for tender types: {}", query)

    db.run[Seq[(ReportTimeframe, UUID, TransactionPaymentType, MonetaryAmount)]](
      sql"#$query".as[(ReportTimeframe, UUID, TransactionPaymentType, MonetaryAmount)],
    ).map { result =>
      result.groupBy { case (time, _, _, _) => time }.transform {
        case (_, byTimeValues) =>
          byTimeValues.groupBy { case (_, locationId, _, _) => locationId }.transform {
            case (_, byLocationValues) =>
              byLocationValues.groupBy { case (_, _, paymentType, _) => paymentType }.transform {
                case (_, byPaymentTypeValues) =>
                  val amounts = byPaymentTypeValues.map { case (_, _, _, amount) => amount }
                  MonetaryAmount.sum(amounts)
              }
          }
      }
    }
  }

  private def baseTenderTypeMap(implicit user: UserContext): Map[TransactionPaymentType, MonetaryAmount] =
    TransactionPaymentType.reportValues.map(_ -> MonetaryAmount(0, user.currency)).toMap
}

object LocationSalesView {
  def apply()(implicit ec: ExecutionContext): LocationSalesView =
    new LocationSalesView()
}
