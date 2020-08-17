package io.paytouch.core.reports.services

import akka.http.scaladsl.server.RequestContext
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ PaginationLinks, UserContext }
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.filters.{ ReportAggrFilters, ReportListFilters, ReportTopFilters }
import io.paytouch.core.reports.queries.{ AverageQuery, SumQuery }
import io.paytouch.core.reports.views.{ ReportAggrView, ReportListView, ReportTopView }
import slick.dbio.DBIO

import scala.concurrent._

class QueryService(implicit val ec: ExecutionContext, val daos: Daos) {

  implicit private val db = daos.db

  def count[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ReportResponse[ReportCount]] =
    db.run(filters.view.aggregationCountQuery(filters)).map { result =>
      ReportResponse(meta = ReportMetadata(filters.interval), data = result)
    }

  def sum[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ReportResponse[ReportFields[filters.view.AggrResult]]] =
    db.run(filters.view.aggregationQuery(filters, SumQuery)).map { result =>
      ReportResponse(meta = ReportMetadata(filters.interval), data = result)
    }

  def average[V <: ReportAggrView](
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ReportResponse[ReportFields[filters.view.AggrResult]]] =
    db.run(filters.view.aggregationQuery(filters, AverageQuery)).map { result =>
      ReportResponse(meta = ReportMetadata(filters.interval), data = result)
    }

  def top[V <: ReportTopView](
      filters: ReportTopFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ReportResponse[filters.view.TopResult]] =
    db.run(filters.view.topQuery(filters)).map { result =>
      ReportResponse(meta = ReportMetadata(filters.interval), data = result)
    }

  def list[V <: ReportListView](
      filters: ReportListFilters[V],
    )(implicit
      ctx: RequestContext,
      user: UserContext,
    ): Future[ReportResponse[filters.view.ListResult]] =
    for {
      count <- db.run(filters.view.countAllQuery(filters))
      result <- db.run(filters.view.listAllQuery(filters))
    } yield {
      val paginationLinks = PaginationLinks(filters.pagination, ctx.request.uri, count)
      ReportResponse(meta = ReportMetadata(filters.interval, Some(paginationLinks)), data = result)
    }

  def single(query: DBIO[Vector[List[String]]]): Future[List[List[String]]] = db.run(query).map(_.toList)

}
