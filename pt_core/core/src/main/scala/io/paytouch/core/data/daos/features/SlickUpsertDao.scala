package io.paytouch.core.data.daos.features

import io.paytouch.core.data.model.upsertions.UpsertionModel
import io.paytouch.core.utils.ResultType

import scala.concurrent._

trait SlickUpsertDao extends SlickDao {
  type Upsertion <: UpsertionModel[Record]

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)]
}
