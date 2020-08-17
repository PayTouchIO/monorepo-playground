package io.paytouch.core.clients.aws

import java.io.File
import java.net.URL
import java.time.ZonedDateTime
import java.util.Date

import scala.concurrent._
import scala.jdk.CollectionConverters._
import scala.util.Try

import awscala.s3.{ Bucket, S3Client => AWSS3Client }

import com.amazonaws._
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model._

class S3Client(implicit val ec: ExecutionContext) {
  protected lazy val s3Client = {
    val s3 = new AWSS3Client
    val usEast1 = regions.Region.getRegion(regions.Regions.US_EAST_1)
    s3.at(usEast1)
  }

  private val s3Url = s"https://s3.amazonaws.com"

  def createOrGetBucket(name: String): Bucket =
    Try {
      s3Client.createBucket(name)
    } getOrElse {
      s3Client.bucket(name) getOrElse {
        throw new IllegalStateException(s"Couldn't create or get bucket $name")
      }
    }

  def uploadPrivateFileToBucket(key: String, file: File)(implicit bucket: Bucket): Future[String] =
    Future {
      bucket.put(key, file)(s3Client)
      toUrl(key)
    }

  def uploadPublicFileToBucket(key: String, file: File)(implicit bucket: Bucket): Future[String] =
    Future {
      bucket.putAsPublicRead(key, file)(s3Client)
      toUrl(key)
    }

  def getAllKeys(implicit bucket: Bucket): Future[Seq[String]] =
    Future {
      s3Client.listObjects(bucket.name).getObjectSummaries.asScala.map(_.getKey).toSeq
    }

  def deleteAllUnderKey(key: String)(implicit bucket: Bucket): Future[Unit] =
    Future {
      val objects = s3Client.listObjects(bucket.name, key).getObjectSummaries.asScala
      objects.foreach(file => s3Client.deleteObject(bucket.name, file.getKey))
    }

  def getPresignedUrl(url: String, expiredDate: ZonedDateTime)(implicit bucket: Bucket): Future[String] =
    Future {
      val key = extractKey(url)
      val expireDate = Date.from(expiredDate.toInstant)
      s3Client.generatePresignedUrl(bucket.name, key, expireDate).toString
    }

  def getPresignedPublicReadUploadUrl(
      key: String,
      expiredDate: ZonedDateTime,
    )(implicit
      bucket: Bucket,
    ): Future[String] =
    Future {
      val expireDate = Date.from(expiredDate.toInstant)
      val generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(bucket.name, key)
          .withMethod(HttpMethod.PUT)
          .withExpiration(expireDate)

      // Ensure the uploaded object has public-read permissions
      generatePresignedUrlRequest.addRequestParameter(
        Headers.S3_CANNED_ACL,
        CannedAccessControlList.PublicRead.toString,
      )

      s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString
    }

  def getUrlKey(key: String)(implicit bucket: Bucket): Future[Option[String]] =
    // FIXME why does it list all keys to check if an object exists instead of `s3Client.get`?
    getAllKeys.map(_.find(_ equalsIgnoreCase key).map(toUrl))

  def getUrlKeyWithoutChecks(key: String)(implicit bucket: Bucket): String =
    toUrl(key)

  private val sep = "/"

  private def toUrl(key: String)(implicit bucket: Bucket): String =
    List(s3Url, bucket.name, key).mkString(sep)

  private def extractKey(url: String): String = {
    val dirs = new URL(url).getPath.split(sep).filter(_.nonEmpty)
    dirs.tail.mkString(sep)
  }

}
