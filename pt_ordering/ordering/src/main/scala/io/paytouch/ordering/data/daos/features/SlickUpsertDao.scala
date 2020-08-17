package io.paytouch.ordering.data.daos.features

import scala.concurrent._

import io.paytouch.ordering.data.model.upsertions.UpsertionModel
import io.paytouch.ordering.Result

trait SlickUpsertDao extends SlickDao {
  type Upsertion <: UpsertionModel[Record]

  def upsert(upsertion: Upsertion): Future[Result[Record]]
}
