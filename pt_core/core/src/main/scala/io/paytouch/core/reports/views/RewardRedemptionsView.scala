package io.paytouch.core.reports.views

import java.util.UUID

import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.{ ReportFields, RewardRedemptionsAggregate }
import io.paytouch.core.reports.filters.ReportFilters
import slick.jdbc.GetResult

object RewardRedemptionsView extends RewardRedemptionsView

trait RewardRedemptionsView extends ReportAggrView {

  type GroupBy = NoGroupBy
  type Field = RewardRedemptionsFields
  type AggrResult = RewardRedemptionsAggregate

  protected val aggrCsvConverter = rewardRedemptionsAggregateConverter

  val fieldsEnum = RewardRedemptionsFields

  val endpoint = "reward_redemptions"

  override def tableName(filters: ReportFilters) =
    s"reports_reward_redemptions_func('${filters.merchantId}')"
  override lazy val tableNameAlias = "reports_reward_redemptions"

  override val groupByInOuterQuery = true

  def aggrResult(implicit user: UserContext) =
    GetResult(r =>
      // IMPORTANT: Ensure fields populated alphabetically!
      for {
        count <- r.nextIntOption()
        data <- Option(RewardRedemptionsAggregate.getResultOrZero(count, r))
        key <- Option(r.nextStringOption())
      } yield ReportFields(values = data, key = key),
    )

  def expandView(filters: ReportFilters): Option[String] = None

  def locationClauses(locationIds: Seq[UUID]) = Seq.empty

  def dateClauses(from: String, to: String): Seq[String] = Seq(s"$tableNameAlias.created_at BETWEEN $from AND $to")

  def defaultJoins(filters: ReportFilters): Seq[String] = {
    val clauses = dateClauses(intervalStartTimeSelector, intervalEndTimeSelector) ++
      locationClauses(filters.locationIds)
    Seq(s"LEFT OUTER JOIN ${tableName(filters)} AS $tableNameAlias ON ${clauses.mkString(" AND ")}")
  }
}
