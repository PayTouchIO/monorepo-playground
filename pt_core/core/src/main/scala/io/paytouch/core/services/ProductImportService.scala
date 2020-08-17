package io.paytouch.core.services

import java.io.File
import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem }

import awscala.s3.Bucket

import cats.implicits._

import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.ImportType
import io.paytouch.core.entities.{ UserContext, Import => ImportEntity }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.{ ImportValidator, LocationValidator }
import io.paytouch.core.{ withTag, S3ImportsBucket }

import scala.concurrent._

class ProductImportService(
    asyncSystem: ActorSystem,
    eventTracker: ActorRef withTag EventTracker,
    s3Client: S3Client,
    setupStepService: SetupStepService,
    uploadBucket: Bucket withTag S3ImportsBucket,
  )(implicit
    ec: ExecutionContext,
    daos: Daos,
  ) extends ImportService(asyncSystem, eventTracker, s3Client, setupStepService, uploadBucket) {

  val importValidator = new ImportValidator
  val locationValidator = new LocationValidator

  def scheduleProductImport(
      id: UUID,
      file: File,
      locationIds: Seq[UUID],
      dryRun: Boolean,
      deleteExisting: Option[Boolean],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ImportEntity]] = {
    val validation = for {
      importRecord <- importValidator.validateOneById(id)
      locationRecords <- locationValidator.accessByIds(locationIds)
    } yield Multiple.combine(importRecord, locationRecords) { case _ => () }

    validation.flatMapTraverse(_ => scheduleImport(id, file, locationIds, dryRun, deleteExisting, ImportType.Product))
  }
}
