package io.paytouch.core.services

import java.io.File
import java.util.UUID

import scala.concurrent._

import akka.actor.{ ActorRef, ActorSystem, Props }
import awscala.s3.Bucket

import cats.implicits._
import cats.data.Validated.{ Invalid, Valid }

import com.sksamuel.scrimage.Image
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.async.uploaders._
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.conversions.ImageUploadConversions
import io.paytouch.core.data.daos.{ Daos, ImageUploadDao }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model
import io.paytouch.core.data.model.{ ImageUploadRecord, ImageUploadUpdate }
import io.paytouch.core.entities
import io.paytouch.core.entities.{
  ImageUploadUpsertionV1,
  ImageUrls,
  ResettableUUID,
  UserContext,
  ImageUpload => ImageUploadEntity,
}
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.features.CreateFeature
import io.paytouch.core.services.features.EnrichFeature
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.ResultType
import io.paytouch.core.validators.ImageUploadValidator

class ImageUploadService(
    val asyncSystem: ActorSystem,
    val cloudfrontImagesDistribution: String withTag CloudfrontImagesDistribution,
    val cloudinaryUrl: String withTag CloudinaryUrl,
    val imageRemover: ActorRef withTag ImageRemover,
    val uploadBucket: Bucket withTag S3ImagesBucket,
    val uploadFolder: String withTag UploadFolder,
    val s3Client: S3Client,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ImageUploadConversions
       with CreateFeature
       with EnrichFeature // TODO change to FindByIdFeature
       with LazyLogging {

  protected val dao = daos.imageUploadDao
  protected val validator = new ImageUploadValidator
  val defaultFilters = NoFilters()

  type Creation = entities.ImageUploadCreation
  type Dao = ImageUploadDao
  type Entity = entities.ImageUpload
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = model.ImageUploadUpdate
  type Record = model.ImageUploadRecord
  type Update = entities.ImageUploadUpsertion
  type Validator = ImageUploadValidator

  def findById(id: UUID)(implicit user: UserContext): Future[Option[ImageUploadEntity]] =
    validator.accessOneById(id).map(_.toOption.map(fromRecordToEntity(_, s3ToCloudFrontConverter)))

  def deleteImageByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID): Future[Seq[UUID]] =
    for {
      uploads <- dao.findByIdsAndMerchantId(ids, merchantId)
      uploadsPerType = uploads.groupBy(_.objectType)
      _ <- Future.sequence(uploadsPerType.map { case (imageType, images) => deleteImages(images.map(_.id), imageType) })
    } yield uploads.map(_.id)

  def uploadComplete(id: UUID)(implicit user: UserContext): Future[Multiple.ErrorsOr[Result[Entity]]] =
    validator.accessOneById(id).flatMap {
      case Valid(record) =>
        for {
          upsertionResult <- markUploadAsCompleteAndUpsert(id, record)
        } yield upsertionResult.map {
          case (resultType, entity) =>
            (resultType, entity)
        }

      case i @ Invalid(_) =>
        Future.successful(i)
    }

  private def toUpsertionWithCloudinaryUrls(record: Record): Future[Multiple.ErrorsOr[Model]] =
    Future.successful {
      val urls = record
        .objectType
        .sizes
        .map { size =>
          size.description -> ImageUploader.cloudinaryUrl(s3Client, uploadBucket, cloudinaryUrl, record, size)
        }
        .toMap

      Multiple.success(
        model.ImageUploadUpdate(
          id = Some(record.id),
          merchantId = None,
          urls = Some(urls),
          fileName = None,
          objectId = None,
          objectType = None,
        ),
      )
    }

  final def markUploadAsCompleteAndUpsert(
      id: UUID,
      record: Record,
    )(implicit
      user: UserContext,
    ): Future[Multiple.ErrorsOr[Result[Entity]]] =
    if (record.urls.isDefined)
      // Don't change anything if URLs are already set on the record
      enrich(record, defaultFilters)(NoExpansions()).map { enrichedRecord =>
        Multiple.success((ResultType.Updated, enrichedRecord))
      }
    else
      // Add cloudinary urls to the image
      toUpsertionWithCloudinaryUrls(record).flatMapTraverse(upsert(id, _))

  //
  // V1 Upload
  //

  def scheduleImageUpload(
      id: UUID,
      upsertion: ImageUploadUpsertionV1,
    )(implicit
      user: UserContext,
    ): Future[Multiple.ErrorsOr[ImageUploadRecord]] = {
    val update = fromUpsertionV1ToUpdate(id, upsertion)
    val imageUploadResp = validator.validateImageUploadUpsertionV1(id, upsertion).flatMap {
      case Valid(img) =>
        dao.upsert(update).map {
          case (_, r) =>
            triggerImageUploads(id, upsertion, img)
            Multiple.success(r)
        }
      case i @ Invalid(_) => Future.successful(i)
    }
    for {
      imgUpload <- imageUploadResp
      _ <- cleanUp(upsertion.file)
    } yield imgUpload
  }

  private def triggerImageUploads(
      id: UUID,
      upsertion: ImageUploadUpsertionV1,
      image: Image,
    ) = {
    val uploadType = upsertion.objectType
    val uploader = createUploader(id)
    val msg = UploadImage(id, image, uploadType)
    uploader ! msg
  }

  private def createUploader(id: UUID) =
    asyncSystem.actorOf(Props(new ImageUploader(s3Client, uploadBucket, uploadFolder)), s"image-uploader-$id")

  private def cleanUp(file: File): Future[Boolean] = Future(file.delete)

  def convertToImageUploadUpdates(
      itemId: UUID,
      imageUploadType: ImageUploadType,
      imageUploadId: ResettableUUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ImageUploadUpdate]]]] =
    convertToImageUploadUpdates(itemId, imageUploadType, imageUploadId.map(_.toSeq))

  def convertToImageUploadUpdates(
      itemId: UUID,
      imageUploadType: ImageUploadType,
      imageUploadIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ImageUploadUpdate]]]] =
    imageUploadIds match {
      case None      => Future.successful(Multiple.empty)
      case Some(Nil) => Future.successful(Multiple.successOpt(Seq.empty[ImageUploadUpdate]))
      case _         => convertToMultipleImageUploadUpdates(Map(itemId -> imageUploadIds), imageUploadType)
    }

  def convertToMultipleImageUploadUpdates(
      relations: Map[UUID, Option[Seq[UUID]]],
      imageUploadType: ImageUploadType,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ImageUploadUpdate]]]] = {
    val imageUploadIds = relations.values.flatten.flatten.toSeq.distinct
    imageUploadIds match {
      case Nil => Future.successful(Multiple.empty)
      case imageIds =>
        validator.accessByIdsAndImageUploadType(imageIds, imageUploadType).mapNested { _ =>
          val updates = relations.flatMap {
            case (itemId, Some(imgIds)) => Some(toImageUploadUpdates(imgIds, itemId))
            case (_, None)              => None
          }.flatten
          Some(updates.toSeq)
        }
    }
  }

  //
  // End V1 Upload
  //

  def deleteImage(id: UUID, imageUploadType: ImageUploadType) =
    deleteImages(Seq(id), imageUploadType)

  def deleteImages(ids: Seq[UUID], imageUploadType: ImageUploadType) = {
    if (ids.nonEmpty) imageRemover ! DeleteImages(ids, imageUploadType)
    dao.deleteByIds(ids)
  }

  def findByObjectId(objectId: UUID, imageUploadType: ImageUploadType): Future[Seq[ImageUrls]] =
    findByObjectIds(Seq(objectId), imageUploadType).map(_.getOrElse(objectId, Seq.empty))

  def findByObjectIds(objectIds: Seq[UUID], imageUploadType: ImageUploadType): Future[Map[UUID, Seq[ImageUrls]]] =
    dao
      .findByObjectIds(objectIds, imageUploadType)
      .map(
        _.filter(_.objectId.isDefined)
          .filter(_.urls.isDefined)
          .groupBy(_.objectId.get)
          .transform((_, v) => v.map(fromRecordToImageUrlsEntity(_, s3ToCloudFrontConverter))),
      )

  def associateImagesToObjectId(
      imageIds: Seq[UUID],
      objectId: UUID,
      merchantId: UUID,
    ) =
    dao.updateObjectId(ids = imageIds, objectId = objectId, merchantId = merchantId)

  private def s3ToCloudFrontConverter(url: String): String =
    url.replace(s"s3.amazonaws.com/${uploadBucket.name}", cloudfrontImagesDistribution)

  def convertToUpsertionModel(id: UUID, update: Update)(implicit user: UserContext): Future[Multiple.ErrorsOr[Model]] =
    Future.successful(Multiple.success(fromUpsertionToUpdate(id, update)))

  def fromRecordToEntity(record: model.ImageUploadRecord)(implicit user: UserContext): entities.ImageUpload =
    fromRecordToEntity(record, s3ToCloudFrontConverter)

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      imageUrls <- getUrlsByImageUpload(records)
      uploadUrls <- getUploadUrlByImageUpload(records)
    } yield fromRecordsAndOptionsToEntities(records, imageUrls, uploadUrls)

  private def getUrlsByImageUpload(records: Seq[Record]): Future[Map[Record, Map[String, String]]] =
    Future.successful {
      records
        .filter(_.urls.isDefined)
        .map(record => record -> record.urls.get.transform((_, v) => s3ToCloudFrontConverter(v)))
        .toMap
    }

  private def getUploadUrlByImageUpload(records: Seq[Record]): Future[Map[Record, String]] = {
    val futures = records
      .filter(_.urls.isEmpty)
      .map { record =>
        ImageUploader.uploadUrl(s3Client, uploadBucket, record).map(url => record -> url)
      }
    Future.sequence(futures).map(_.toMap)
  }

  override implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      enrichedRecord <- enrich(record, defaultFilters)(NoExpansions())
    } yield (resultType, enrichedRecord)
}
