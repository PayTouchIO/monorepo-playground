package io.paytouch.core.reports.services

import java.util.UUID

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.directives.OnSuccessMagnet

import awscala.s3.Bucket

import cats.implicits._

import com.softwaremill.macwire._

import io.paytouch.core.{ withTag, S3ExportsBucket }
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.async.exporters._
import io.paytouch.core.reports.conversions.ExportConversions
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.reports.entities.{ Export, ExportDownload }
import io.paytouch.core.reports.entities.enums.CsvExports
import io.paytouch.core.reports.filters._
import io.paytouch.core.reports.queries.CsvExport
import io.paytouch.core.reports.validators.ExportValidator
import io.paytouch.core.reports.views.{ ReportAggrView, ReportListView, ReportTopView, ReportView }
import io.paytouch.core.utils.{ UpsertionResult, UtcTime }
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class ExportService(
    val asyncSystem: ActorSystem,
    val queryService: QueryService,
    val s3Client: S3Client,
    val uploadBucket: Bucket withTag S3ExportsBucket,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ExportConversions {

  type Record = ExportRecord
  type Entity = Export
  type Validator = ExportValidator

  protected val dao = daos.exportDao
  protected val validator = new ExportValidator

  private val PresignedExpirationInMins = 5

  def findById(id: UUID)(implicit user: UserContext): Future[Option[Entity]] =
    dao.findByIdAndMerchantId(id, user.merchantId)

  def generatePresignedUrl(id: UUID)(implicit user: UserContext): Future[ErrorsOr[ExportDownload]] =
    validator.validateDownloadBaseUrl(id).flatMapTraverse { baseUrl =>
      s3Client
        .getPresignedUrl(baseUrl, UtcTime.now.plusMinutes(PresignedExpirationInMins))(uploadBucket)
        .map(ExportDownload)
    }

  def count[V <: ReportAggrView](
      filename: String,
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    createAndScheduleExport(filters)(CountReport(_, filename, filters, _))

  def sum[V <: ReportAggrView](
      filename: String,
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    createAndScheduleExport(filters)(SumReport(_, filename, filters, _))

  def average[V <: ReportAggrView](
      filename: String,
      filters: ReportAggrFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    createAndScheduleExport(filters)(AverageReport(_, filename, filters, _))

  def top[V <: ReportTopView](
      filename: String,
      filters: ReportTopFilters[V],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    createAndScheduleExport(filters)(TopReport(_, filename, filters, _))

  def list[V <: ReportListView](
      filename: String,
      filters: ReportListFilters[V],
    )(implicit
      ctx: RequestContext,
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    createAndScheduleExport(filters)(ListReport(_, filename, filters, ctx, _))

  def single[T <: CsvExports](
      csvExport: T,
      filename: String,
    )(implicit
      user: UserContext,
      csvExportQuery: CsvExport[T],
    ): Future[ErrorsOr[Result[Entity]]] =
    for {
      (resultType, export) <- createExport(csvExport)
      msg = CsvMsg(export.id, csvExport, filename, user, csvExportQuery)
      _ <- scheduleCsvExport(export, msg)
    } yield UpsertionResult(resultType -> export)

  private def createAndScheduleExport(
      filters: ReportFilters,
    )(
      msgBuilder: (UUID, UserContext) => ReportMsg,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    validator.validateFilters(filters).flatMapTraverse { location =>
      for {
        (resultType, export) <- createExport(filters.view)
        msg = msgBuilder(export.id, user)
        _ <- scheduleReportExport(export, msg)
      } yield resultType -> export
    }

  private def createExport(view: ReportView)(implicit userContext: UserContext): Future[(ResultType, Entity)] =
    dao.upsert(toExportUpdate(view.endpoint))

  private def createExport(csvExport: CsvExports)(implicit userContext: UserContext): Future[(ResultType, Entity)] =
    dao.upsert(toExportUpdate(csvExport.entryName))

  private def scheduleReportExport(export: Export, msg: ReportMsg): Future[Unit] =
    Future.successful {
      val exporter = asyncSystem.actorOf(Props(wire[ReportExporter]), s"report-exporter-${export.id}")
      exporter ! msg
    }

  private def scheduleCsvExport(export: Export, msg: CsvMsg[_]): Future[Unit] =
    Future.successful {
      val exporter = asyncSystem.actorOf(Props(wire[CsvExporter]), s"csv-exporter-${export.id}")
      exporter ! msg
    }

}
