package io.paytouch.core.reports.async.exporters

import java.io.File

import akka.actor.{ Actor, ActorLogging, PoisonPill }
import awscala.s3.Bucket
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.Pagination
import io.paytouch.core.reports.services.QueryService
import io.paytouch.core.{ withTag, S3ExportsBucket }

import scala.concurrent._
import scala.util.{ Failure, Success }

class CsvExporter(
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
    case msg: CsvMsg[_] => processAndUploadPaginated(msg)
  }

  private def processAndUploadPaginated(implicit msg: CsvMsg[_]) =
    processAndUpload(processPaginatedDataToFile)

  private def processAndUpload[T](processorToFile: => Future[File])(implicit msg: CsvMsg[_]) = {
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
        log.error(ex, s"[Csv Export $id] Failure: $ex")
        exportDao.exportFailed(id) onComplete (_ => suicide)
    }
  }

  private def processPaginatedDataToFile(implicit msg: CsvMsg[_]): Future[File] = {
    val csvExport = msg.cvsExport

    def paginatedQueryRunner: Pagination => Future[List[List[String]]] = { pagination =>
      queryService.single(csvExport.paginatedQuery(msg.user.merchantId, pagination))
    }

    def loopPaginationAndWriteRows(file: File, pagination: Pagination): Future[Unit] =
      paginatedQueryRunner(pagination).flatMap { response =>
        if (response.isEmpty) Future.unit
        else
          for {
            _ <- csvConverter.addRows(file, response)
            newPagination = pagination.copy(page = pagination.page + 1)
            _ <- loopPaginationAndWriteRows(file, newPagination)
          } yield ()
      }

    for {
      file <- csvConverter.createTempFile
      _ <- loopPaginationAndWriteRows(file, Pagination(1, 99))
      _ <- csvConverter.prependHeaderAndRemoveIgnorableColumns(file, csvExport.header)
    } yield file
  }

  private def uploadFile[T](file: File)(implicit msg: CsvMsg[_]): Future[String] = {
    val merchantId = msg.user.merchantId
    val exportId = msg.exportId
    val fileName = s"${msg.buildFilename()}.csv"
    val keys = Seq(merchantId.toString, msg.csvExportType.entryName, exportId, fileName)
    for {
      _ <- exportDao.exportUploading(exportId)
      url <- s3Client.uploadPrivateFileToBucket(keys.mkString("/"), file)
    } yield url
  }

  def suicide = self ! PoisonPill

}
