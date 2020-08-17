package io.paytouch.core.async.uploaders

import java.util.UUID

import akka.actor.{ Actor, ActorLogging }
import awscala.s3.Bucket
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.{ withTag, S3ImagesBucket }

import scala.concurrent._

final case class DeleteImages(ids: Seq[UUID], imageUploadType: ImageUploadType)

class ImageRemover(s3Client: S3Client, uploadBucket: Bucket withTag S3ImagesBucket)(implicit daos: Daos)
    extends Actor
       with ActorLogging {

  implicit val ec = context.dispatcher

  val imageUploadDao = daos.imageUploadDao

  implicit private lazy val bucket = uploadBucket

  def receive = {
    case DeleteImages(ids, imageUploadType) => deleteImages(ids, imageUploadType)
  }

  def deleteImages(ids: Seq[UUID], imageUploadType: ImageUploadType) =
    retrieveKeysToDelete(ids, imageUploadType).map(_.map { keyToDelete =>
      log.info(s"[AWS S3] Deleting all objects under key $keyToDelete")
      s3Client.deleteAllUnderKey(keyToDelete)
    })

  private def retrieveKeysToDelete(ids: Seq[UUID], imageUploadType: ImageUploadType): Future[Seq[String]] =
    if (ids.nonEmpty)
      Future.successful(Seq.empty)
    else
      s3Client.getAllKeys.map { keys =>
        val partialKeys = ids.map(_.toString)
        val imageUploadTypeBucket = imageUploadType.bucketName
        keys.filter(k => partialKeys.exists(pk => k.containsSlice(s"$imageUploadTypeBucket/$pk")))
      }
}
