package io.paytouch.core.data.daos.features

import java.util.UUID

import io.paytouch.core.filters.BaseFilters

import scala.concurrent._

trait SlickFindAllDao extends SlickCommonDao {
  type Filters <: BaseFilters

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]]

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int]
}
