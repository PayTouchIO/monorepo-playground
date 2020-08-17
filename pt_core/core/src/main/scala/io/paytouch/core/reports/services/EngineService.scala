package io.paytouch.core.reports.services

import akka.http.scaladsl.server.RequestContext

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.EngineResponse
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportListFilters, ReportTopFilters }
import io.paytouch.core.reports.validators.FiltersValidator
import io.paytouch.core.reports.views.{ ReportAggrView, ReportListView, ReportTopView }

import scala.concurrent._

class EngineService(val queryService: QueryService)(implicit val ec: ExecutionContext, val daos: Daos) {

  implicit private val db = daos.db

  val filtersValidator = new FiltersValidator

  // TODO - somehow remove code duplication ?

  def count[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[EngineResponse[ReportCount]] =
    filtersValidator.validateFilters(filters).flatMapTraverse(implicit location => queryService.count(filters))

  def sum[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[EngineResponse[ReportFields[filters.view.AggrResult]]] =
    filtersValidator.validateFilters(filters).flatMapTraverse(implicit location => queryService.sum(filters))

  def average[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[EngineResponse[ReportFields[filters.view.AggrResult]]] =
    filtersValidator.validateFilters(filters).flatMapTraverse(implicit location => queryService.average(filters))

  def top[V <: ReportTopView](
      filters: ReportTopFilters[V],
    )(implicit
      user: UserContext,
    ): Future[EngineResponse[filters.view.TopResult]] =
    filtersValidator.validateFilters(filters).flatMapTraverse(implicit location => queryService.top(filters))

  def list[V <: ReportListView](
      filters: ReportListFilters[V],
    )(implicit
      ctx: RequestContext,
      user: UserContext,
    ): Future[EngineResponse[filters.view.ListResult]] =
    filtersValidator.validateFilters(filters).flatMapTraverse(implicit location => queryService.list(filters))

}
