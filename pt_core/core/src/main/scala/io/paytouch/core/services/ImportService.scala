package io.paytouch.core.services

import java.io.File
import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, Props }
import awscala.s3.Bucket
import com.softwaremill.macwire.wire
import io.paytouch.core.async.importers.{ ParseAndLoadData, ParseData, ProductImporter }
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.conversions.ImportConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.data.model.enums.ImportType
import io.paytouch.core.entities.{ UserContext, Import => ImportEntity }
import io.paytouch.core.{ withTag, S3ImportsBucket }

import scala.concurrent._

class ImportService(
    val asyncSystem: ActorSystem,
    val eventTracker: ActorRef withTag EventTracker,
    val s3Client: S3Client,
    val setupStepService: SetupStepService,
    val uploadBucket: Bucket withTag S3ImportsBucket,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ImportConversions {

  protected val dao = daos.importDao

  def scheduleImport(
      id: UUID,
      file: File,
      locationIds: Seq[UUID],
      dryRun: Boolean,
      deleteExisting: Option[Boolean],
      `type`: ImportType,
    )(implicit
      user: UserContext,
    ): Future[ImportEntity] = {
    val importUpdate = toImportUpdate(id, file, locationIds, deleteExisting, `type`)
    dao.upsert(importUpdate).map {
      case (_, importRecord) =>
        val importer = createImporter(importRecord)
        val msg = if (dryRun) ParseData(importRecord.id) else ParseAndLoadData(importRecord.id)
        importer ! msg
        fromRecordToEntity(importRecord)
    }
  }

  private def createImporter(importRecord: ImportRecord) =
    importRecord.`type` match {
      case ImportType.Product =>
        asyncSystem.actorOf(Props(wire[ProductImporter]), s"product-importer-${importRecord.id}")
    }

  def findById(id: UUID)(implicit user: UserContext): Future[Option[ImportEntity]] =
    dao.findByIdAndMerchantId(id, user.merchantId)
}
