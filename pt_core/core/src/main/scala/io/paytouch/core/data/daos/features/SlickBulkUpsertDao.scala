package io.paytouch.core.data.daos.features

import io.paytouch.core.data.model.upsertions.UpsertionModel

import scala.concurrent._

trait SlickBulkUpsertDao extends SlickDao {
  type Upsertion <: UpsertionModel[Record]

  def bulkUpsert(upsertion: Upsertion): Future[Seq[Record]]
}
