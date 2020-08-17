package io.paytouch.core.async.importers

import java.io.File
import java.util.UUID

import scala.concurrent._
import scala.util.{ Failure, Success }

import akka.actor.{ Actor, PoisonPill }
import awscala.s3.Bucket

import cats.data._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.{ withTag, S3ImportsBucket }
import io.paytouch.core.async.importers.loaders.Loader
import io.paytouch.core.async.importers.parsers.{ Parser, ValidationResult }
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.json.JsonSupport

final case class ParseData(importId: UUID)
final case class ParseAndLoadData(importId: UUID)

abstract class Importer(implicit daos: Daos) extends Actor with LazyLogging with JsonSupport {
  import context.dispatcher

  type Data

  def parser: Parser[Data]
  def loader: Loader[Data]

  def s3Client: S3Client
  def uploadBucket: Bucket withTag S3ImportsBucket
  implicit private lazy val bucket = uploadBucket

  val importDao = daos.importDao

  def receive = {
    case ParseData(id)        => parse(id) onComplete (_ => clean(id))
    case ParseAndLoadData(id) => parseAndLoad(id) onComplete (_ => clean(id))
  }

  def parse(importId: UUID): Future[Option[(ImportRecord, Data)]] =
    importDao.validationInProgress(importId).flatMap { _ =>
      parser.parse(importId).map {
        case Validated.Invalid(validationErrors) =>
          val validationResult = ValidationResult(validationErrors)
          val validationResultAsJson = fromEntityToJValue(validationResult)
          importDao.validationFailed(importId, validationResultAsJson)

          None

        case Validated.Valid((importer, importSummary, data)) =>
          val importSummaryAsJson = fromEntityToJValue(importSummary)
          importDao.validationSuccessful(importId, importSummaryAsJson)

          Some(importer -> data)
      }
    }

  def load(importer: ImportRecord, data: Data): Future[Unit] = {
    val loading = loader.load(importer, data)

    loading.onComplete {
      case Success(_) => importDao.importSuccessful(importer.id)
      case Failure(ex) =>
        logger.error(s"Import ${importer.id} failed", ex)
        importDao.importFailed(importer.id)
    }

    loading
  }

  def parseAndLoad(importId: UUID) =
    parse(importId).flatMap {
      case Some((importer, data)) => Future.sequence(Seq(uploadFileTos3(importer), load(importer, data)))
      case None =>
        Future.failed {
          val errMsg = s"Failed validation for import $importId. Did you do a dry run first?"
          logger.error(errMsg)
          new RuntimeException(errMsg)
        }
    }

  private def clean(id: UUID) = deleteFilePerImport(id).onComplete(_ => self ! PoisonPill)

  private def uploadFileTos3(importer: ImportRecord): Future[String] = {
    val file = new File(importer.filename)
    val keys = Seq(importer.merchantId, "imports", s"${importer.id}.csv")
    logger.info(s"[Import File Upload ${importer.id}] uploading csv file to S3")
    s3Client.uploadPrivateFileToBucket(keys.mkString("/"), file)
  }

  def deleteFilePerImport(id: UUID) =
    importDao.findById(id).map {
      _.map { importRecord =>
        val filename = importRecord.filename
        new File(filename).delete()
      }
    }
}
