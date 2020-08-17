package io.paytouch.core.reports.views

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext

class LocationGiftCardPassView(implicit val ec: ExecutionContext)
    extends ReportListView
       with ReportAggrView
       with ExtendedView[LocationGiftCardPassView]
       with LazyLogging {
  import io.paytouch.core.data.driver.CustomPlainImplicits._

  type GroupBy = NoGroupBy
  type Field = LocationGiftCardPassFields
  type AggrResult = LocationGiftCardPasses

  type OrderBy = LocationGiftCardPassesOrderByFields
  type ListResult = AggrResult

  val listResultConverter = locationGiftCardPassesConverter
  protected val aggrCsvConverter = locationGiftCardPassesConverter

  val orderByEnum = LocationGiftCardPassesOrderByFields
  val fieldsEnum = LocationGiftCardPassFields
  val endpoint = "location_gift_card_passes"

  override def tableName(filters: ReportFilters) = GiftCardPassView.tableName(filters)
  override lazy val tableNameAlias: String = GiftCardPassView.tableNameAlias

  override lazy val idColumn = s"locations.id"
  override val countSelectors = Seq(s"$idColumn AS id", s"COUNT(DISTINCT $tableNameAlias.gift_card_pass_id) AS cnt")

  val listTable = "locations"

  def listWhereClauses(filters: ReportFilters) = {
    val userAccessibleLocationIds = filters.locationIds
    val filterByLocationIds = filters.ids.getOrElse(userAccessibleLocationIds)
    Seq(s"locations.id IN (${filterByLocationIds.asInParametersList})")
  }

  def aggrResult(implicit user: UserContext): GetResult[Option[ReportFields[LocationGiftCardPasses]]] =
    GetResult(r =>
      for {
        values <- listResult.apply(r)
      } yield ReportFields(values = values, key = Some(values.id.toString)),
    )

  protected def listResult(implicit user: UserContext): GetResult[Option[LocationGiftCardPasses]] =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        addressLine1 <- Option(r.nextStringOption())
        id <- Option(r.nextUUID())
        name <- r.nextStringOption()
        data <- Option(GiftCardPassAggregate.getResultOrZero(count, r))
      } yield LocationGiftCardPasses(id = id, name = name, addressLine1 = addressLine1, data = data),
    )

  def locationClauses(locationIds: Seq[UUID]): Seq[String] =
    Seq(s"$tableNameAlias.original_location_id IN (${locationIds.asInParametersList})")

  def dateClauses(from: String, to: String): Seq[String] = GiftCardPassView.dateClauses(from, to)

  def defaultJoins(filters: ReportFilters) = {
    val onClauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      Seq(s"locations.id = $tableNameAlias.original_location_id") ++ locationClauses(
      filters.locationIds,
    ) ++ GiftCardPassView.defaultClauses

    val whereClauses = listWhereClauses(filters)
    Seq(
      "CROSS JOIN locations",
      s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${onClauses.mkString(" AND ")}",
      GiftCardPassView.transactionsJoinLateral,
      s"WHERE ${whereClauses.mkString(" AND ")}",
    )
  }
}

object LocationGiftCardPassView {
  def apply()(implicit ec: ExecutionContext): LocationGiftCardPassView =
    new LocationGiftCardPassView()
}
