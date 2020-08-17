package io.paytouch.ordering.data.model

import io.paytouch.ordering.data.daos.features.SlickStoreDao
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import slick.lifted.{ CanBeQueryCondition, Rep }

trait SlickRelDao extends SlickStoreDao {

  protected def requires(fields: (String, Option[Any])*) = {
    val context = fields.toMap.keys.mkString(" and ")
    fields.foreach {
      case (k, v) =>
        if (v.isEmpty) {
          val msg = s"$getClass - Impossible to find by $context without a $k"
          require(v.isDefined, msg)
        }
    }
  }

  def queryByRelIds(upsertion: Update): Query[Table, Record, Seq]

  def queryUpsertByRelIds(upsertion: Update) = queryUpsertByQuery(upsertion, queryByRelIds(upsertion))

  def queryBulkUpsertByRelIds(upsertions: Seq[Update]) =
    queryBulkUpsertByQuery(upsertions, queryByRelIds)

  def queryBulkUpsertAndDeleteTheRestByRelIds[R <: Rep[_], S <: Effect](
      upsertions: Seq[Update],
      query: Table => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ) =
    for {
      us <- queryBulkUpsertByRelIds(upsertions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, query)
    } yield records

  def bulkUpsertByRelIds(upsertions: Seq[Update]) =
    runWithTransaction(queryBulkUpsertByRelIds(upsertions))

  def upsertByRelIds(upsertion: Update) =
    runWithTransaction(queryUpsertByRelIds(upsertion))

  def findByRelIds(upsertion: Update) = run(queryByRelIds(upsertion).result.headOption)
}
