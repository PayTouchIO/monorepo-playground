package io.paytouch.core.reports.views

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.{ CardTransactionResultType, TransactionPaymentType, TransactionType }
import io.paytouch.core.entities.{ MonetaryAmount, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportFilters, ReportListFilters }
import io.paytouch.core.reports.queries.EnrichResult
import slick.jdbc.GetResult

import scala.concurrent._

class EmployeeSalesView(implicit val ec: ExecutionContext)
    extends ReportListView
       with ExtendedView[EmployeeSalesView]
       with LazyLogging {
  import io.paytouch.core.data.driver.CustomPlainImplicits._

  type OrderBy = EmployeeSalesOrderByFields
  type ListResult = EmployeeSales

  val listResultConverter = employeeSalesConverter

  val orderByEnum = EmployeeSalesOrderByFields
  val fieldsEnum = EmployeeSalesFields
  val endpoint = "employee_sales"

  override def tableName(filters: ReportFilters) = OrderView.tableName(filters)
  override lazy val tableNameAlias: String = OrderView.tableNameAlias

  override lazy val idColumn = s"users.id"

  val listTable = "users"

  def listWhereClauses(filters: ReportFilters) = {
    val merchantIdFilter = Seq(s"users.merchant_id = '${filters.merchantId}'")

    val byIdFilter = filters.ids.map(ids => Seq(s"users.id IN (${ids.asInParametersList})")).getOrElse(Seq.empty)

    merchantIdFilter ++ byIdFilter
  }

  def aggrResult(implicit user: UserContext): GetResult[Option[ReportFields[EmployeeSales]]] =
    GetResult(r =>
      for {
        values <- listResult.apply(r)
      } yield ReportFields(values = values, key = Some(values.id.toString)),
    )

  protected def listResult(implicit user: UserContext): GetResult[Option[EmployeeSales]] =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        firstName <- r.nextStringOption()
        id <- r.nextUUIDOption()
        lastName <- r.nextStringOption()
        data <- Option(SalesAggregate.getResultOrZero(count, r))
      } yield EmployeeSales(id = id, firstName = firstName, lastName = lastName, data = data),
    )

  def locationClauses(locationIds: Seq[UUID]): Seq[String] =
    Seq(s"$tableNameAlias.location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = OrderView.dateClauses(from, to)

  def defaultJoins(filters: ReportFilters) = {
    val onClauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      Seq(s"users.id = $tableNameAlias.employee_id") ++ locationClauses(filters.locationIds) ++ OrderView
      .defaultClauses(filters, withVoided = true)

    val whereClauses = listWhereClauses(filters)
    Seq("CROSS JOIN users") ++
      Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${onClauses.mkString(" AND ")}") ++
      Seq(s"WHERE ${whereClauses.mkString(" AND ")}")
  }

}

object EmployeeSalesView {
  def apply()(implicit ec: ExecutionContext): EmployeeSalesView =
    new EmployeeSalesView()
}
