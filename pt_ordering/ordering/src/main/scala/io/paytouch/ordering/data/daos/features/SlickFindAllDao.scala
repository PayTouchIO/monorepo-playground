package io.paytouch.ordering.data.daos.features

import java.util.UUID

import io.paytouch.ordering.filters.BaseFilters

import scala.concurrent.Future

trait SlickFindAllDao extends SlickCommonDao {

  type Filters <: BaseFilters

  def findAllWithFilters(contextIds: Seq[UUID], filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]]

  def countAllWithFilters(contextIds: Seq[UUID], filters: Filters): Future[Int]
}
