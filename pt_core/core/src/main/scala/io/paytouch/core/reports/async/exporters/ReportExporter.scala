package io.paytouch.core.reports.async.exporters

import java.io.File

import akka.actor.{ Actor, ActorLogging, PoisonPill }
import awscala.s3.Bucket
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.Pagination
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities.ReportResponse
import io.paytouch.core.reports.services.QueryService
import io.paytouch.core.reports.views.ReportListView
import io.paytouch.core.{ withTag, S3ExportsBucket }

import scala.concurrent._
import scala.util.{ Failure, Success }

class ReportExporter(
    val queryService: QueryService,
    val s3Client: S3Client,
    val uploadBucket: Bucket withTag S3ExportsBucket,
  )(implicit
    daos: Daos,
  ) extends Actor
       with ActorLogging {

  implicit val ec = context.dispatcher

  implicit private lazy val bucket = uploadBucket
  private val csvConverter = new CSVConverter

  val exportDao = daos.exportDao

  def receive: Receive = {
    case msg: CountReport[_] =>
      implicit val m = msg
      simpleProcessAndUpload(queryService.count(msg.filters)(msg.user))
    case msg: SumReport[_] =>
      implicit val m = msg
      implicit val converter = msg.filters.view.reportFieldsConverter
      simpleProcessAndUpload(queryService.sum(msg.filters)(msg.user))
    case msg: AverageReport[_] =>
      implicit val m = msg
      implicit val converter = msg.filters.view.reportFieldsConverter
      simpleProcessAndUpload(queryService.average(msg.filters)(msg.user))
    case msg: TopReport[_] =>
      implicit val m = msg
      implicit val innerConverter = msg.filters.view.topCSVConverter
      simpleProcessAndUpload(queryService.top(msg.filters)(msg.user))
    case msg: ListReport[_] => processAndUploadPaginated(msg)
  }

  private def simpleProcessAndUpload[T](
      queryRunner: => Future[ReportResponse[T]],
    )(implicit
      msg: ReportMsg,
      converter: CSVConverterHelper[T],
    ) = {
    def processToFile: Future[ReportResponse[T]] => Future[File] = { responseF =>
      for {
        response <- responseF
        file <- csvConverter.createTempFile
        reportResponseConverter = converterBuilder(converter)
        (header, rows) <- csvConverter.convertToData(response, msg.filters)(reportResponseConverter)
        _ <- csvConverter.addRows(file, rows)
        _ <- csvConverter.prependHeaderAndRemoveIgnorableColumns(file, header)
      } yield file
    }
    processAndUpload(processToFile(queryRunner))
  }

  private def processAndUploadPaginated[V <: ReportListView](implicit msg: ListReport[V]) =
    processAndUpload(processPaginatedDataToFile)

  private def processAndUpload[T](processorToFile: => Future[File])(implicit msg: ReportMsg) = {
    val result = for {
      _ <- exportDao.exportProcessing(msg.exportId)
      file <- processorToFile
      url <- uploadFile(file)
      _ <- exportDao.exportCompleted(msg.exportId, url)
      _ <- Future(file.delete())
    } yield ()
    result onComplete {
      case Success(_) => suicide
      case Failure(ex) =>
        val id = msg.exportId
        log.error(ex, s"[Export $id] Failure: $ex")
        exportDao.exportFailed(id) onComplete (_ => suicide)
    }
  }

  private def processPaginatedDataToFile[V <: ReportListView](implicit msg: ListReport[V]): Future[File] = {
    implicit val reportResponseConverter = converterBuilder(msg.filters.view.listResultConverter)

    def paginatedQueryRunner: Pagination => Future[ReportResponse[msg.filters.view.ListResult]] = { pagination =>
      val filterWithPagination = msg.filters.copy(pagination = pagination)
      val result = queryService.list(filterWithPagination)(msg.ctx, msg.user)
      // TBD - Terrible! Can we do better?
      result.asInstanceOf[Future[ReportResponse[msg.filters.view.ListResult]]]
    }

    def loopPaginationAndWriteRows(
        file: File,
        pagination: Pagination,
        maxHeader: List[String],
      ): Future[List[String]] =
      paginatedQueryRunner(pagination).flatMap { response =>
        if (response.data.isEmpty) Future.successful(maxHeader)
        else
          for {
            (header, rows) <- csvConverter.convertToData(response, msg.filters)(reportResponseConverter)
            _ <- csvConverter.addRows(file, rows)
            newMaxHeader = if (header.size > maxHeader.size) header else maxHeader
            newPagination = pagination.copy(page = pagination.page + 1)
            finalHeader <- loopPaginationAndWriteRows(file, newPagination, newMaxHeader)
          } yield finalHeader
      }

    for {
      file <- csvConverter.createTempFile
      header <- loopPaginationAndWriteRows(file, Pagination(1, 99), List.empty)
      _ <- csvConverter.prependHeaderAndRemoveIgnorableColumns(file, header)
    } yield file
  }

  private def uploadFile[T](file: File)(implicit msg: ReportMsg): Future[String] = {
    val merchantId = msg.user.merchantId
    val exportId = msg.exportId
    val viewType = msg.filters.view.endpoint
    val dates = Seq(msg.filters.from, msg.filters.to).map(_.toLocalDate.toString).distinct
    val filenamePieces = Seq(msg.filename) ++ dates
    val fileName = s"${filenamePieces.mkString("_")}.csv"
    val keys = Seq(merchantId.toString, viewType, exportId, fileName)
    for {
      _ <- exportDao.exportUploading(exportId)
      url <- s3Client.uploadPrivateFileToBucket(keys.mkString("/"), file)
    } yield url
  }

  def suicide = self ! PoisonPill

}
