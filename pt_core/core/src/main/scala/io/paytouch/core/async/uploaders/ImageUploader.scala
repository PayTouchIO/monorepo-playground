package io.paytouch.core.async.uploaders

import java.io.File
import java.util.UUID

import akka.actor.{ Actor, ActorLogging, PoisonPill }
import awscala.s3.Bucket
import cats.data.Validated.{ Invalid, Valid }
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.PngWriter
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.enums.ImageSize
import io.paytouch.core.errors.NonAccessibleImageUploadIds
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.{ withTag, S3ImagesBucket, UploadFolder }

import scala.concurrent._
import io.paytouch.core.utils.UtcTime
import java.net.URLEncoder

final case class UploadImage(
    id: UUID,
    image: Image,
    uploadType: ImageUploadType,
  )

class ImageUploader(
    val s3Client: S3Client,
    val uploadBucket: Bucket withTag S3ImagesBucket,
    val uploadFolder: String withTag UploadFolder,
  )(implicit
    daos: Daos,
  ) extends Actor
       with ActorLogging {

  implicit val ec = context.dispatcher

  val imageUploadDao = daos.imageUploadDao

  implicit private lazy val bucket = uploadBucket

  def receive = {
    case UploadImage(id, image, uploadType) =>
      uploadImage(id, image, uploadType).onComplete(_ => killYourself)
  }

  private def uploadImage(
      id: UUID,
      image: Image,
      uploadType: ImageUploadType,
    ) =
    resizeAndUploadToS3(id, image, uploadType).map {
      case Valid(urls)    => updateImageUploadRecord(id, urls)
      case i @ Invalid(_) => log.error(s"[Image upload $id] failed! $i")
    }

  private def resizeAndUploadToS3(
      id: UUID,
      image: Image,
      uploadType: ImageUploadType,
    ): Future[ErrorsOr[Map[ImageSize, String]]] =
    imageUploadDao.findById(id).flatMap {
      case Some(record) => generateAndUploadPngImages(record, image, uploadType)
      case None         => Future.successful(Multiple.failure(NonAccessibleImageUploadIds(Seq(id))))
    }

  private def updateImageUploadRecord(id: UUID, urls: Map[ImageSize, String]): Future[Boolean] = {
    log.info(s"[Image Upload $id] storing S3 urls")
    val imgSizeMap = urls.map { case (k, v) => (k.description, v) }
    imageUploadDao.updateUrls(id, imgSizeMap)
  }

  private def generateAndUploadPngImages(
      imageUpload: ImageUploadRecord,
      image: Image,
      uploadType: ImageUploadType,
    ): Future[ErrorsOr[Map[ImageSize, String]]] = {
    val sizes = uploadType.sizes
    val uploads = sizes.map(imgSize => resizeAndUploadToS3(imageUpload, imgSize, image, uploadType))
    Future.sequence(uploads).map(m => Multiple.success(m.toMap))
  }

  private def resizeAndUploadToS3(
      imageUpload: ImageUploadRecord,
      imgSize: ImageSize,
      image: Image,
      uploadType: ImageUploadType,
    ): Future[(ImageSize, String)] =
    for {
      file <- resizeImageToFile(imageUpload, imgSize, image)
      url <- uploadToS3(imageUpload, imgSize, uploadType, file)
      _ <- cleanUp(file)
    } yield (imgSize, url)

  private def resizeImageToFile(
      imageUpload: ImageUploadRecord,
      imgSize: ImageSize,
      image: Image,
    ): Future[File] =
    Future {
      log.info(s"[Image Upload ${imageUpload.id}] resizing and converting image to $imgSize")
      val outputFile = new File(s"$uploadFolder/image.updload.${imageUpload.id}.${imgSize.description}.png")
      val coveredImg = imgSize.size.map(n => image.cover(n, n)) getOrElse image
      coveredImg.output(outputFile)(PngWriter())
      log.info(s"[Image Upload ${imageUpload.id}] resized and converted image to $imgSize")
      outputFile
    }

  private def uploadToS3(
      imageUpload: ImageUploadRecord,
      imgSize: ImageSize,
      uploadType: ImageUploadType,
      file: File,
    ): Future[String] = {
    val fileName = imgSize.size.map(n => s"${n}x$n") getOrElse imgSize.description
    val keys = ImageUploader.bucketKeys(imageUpload, uploadType) ++ Seq(s"$fileName.png")
    log.info(s"[Image Upload ${imageUpload.id}] uploading file $imgSize to S3")
    s3Client.uploadPublicFileToBucket(keys.mkString("/"), file)
  }

  private def cleanUp(file: File): Future[Boolean] = Future(file.delete)

  private def killYourself = self ! PoisonPill
}

object ImageUploader {
  val PresignedExpirationInMins = 5

  def uploadUrl(
      s3client: S3Client,
      uploadBucket: Bucket withTag S3ImagesBucket,
      imageUpload: ImageUploadRecord,
    ): Future[String] = {
    val key = uploadBucketKey(imageUpload)
    val expiration = UtcTime.now.plusMinutes(PresignedExpirationInMins)

    s3client.getPresignedPublicReadUploadUrl(uploadBucketKey(imageUpload), expiration)(uploadBucket)
  }

  def cloudinaryUrl(
      s3Client: S3Client,
      uploadBucket: Bucket withTag S3ImagesBucket,
      cloudinaryBaseUrl: String,
      imageUpload: ImageUploadRecord,
      size: ImageSize,
    ): String = {
    val s3Url = s3Client.getUrlKeyWithoutChecks(uploadBucketKey(imageUpload))(uploadBucket)
    Seq(cloudinaryBaseUrl, "image", "fetch", size.cloudinaryFormatString, URLEncoder.encode(s3Url, "UTF-8"))
      .mkString("/")
  }

  private def uploadBucketKey(imageUpload: ImageUploadRecord): String =
    (bucketKeys(imageUpload, imageUpload.objectType) ++ Seq(imageUpload.fileName)).mkString("/")

  private def bucketKeys(imageUpload: ImageUploadRecord, uploadType: ImageUploadType): Seq[String] =
    Seq(imageUpload.merchantId.toString, uploadType.bucketName, imageUpload.id.toString)
}
