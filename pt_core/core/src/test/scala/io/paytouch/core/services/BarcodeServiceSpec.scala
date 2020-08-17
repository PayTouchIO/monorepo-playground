package io.paytouch.core.services

import java.util.UUID

import scala.jdk.CollectionConverters._

import awscala.s3.{ Bucket, S3Client => AWSS3Client }

import com.amazonaws.services.s3.model.{ ObjectListing, S3ObjectSummary }

import io.paytouch.core.{ S3ImagesBucket, ServiceConfigurations => Config }
import io.paytouch.core.barcodes.entities.BarcodeMetadata
import io.paytouch.core.barcodes.services.BarcodeService
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.utils.Tagging._

class BarcodeServiceSpec extends ServiceDaoSpec {
  abstract class BarcodeServiceSpecContext extends ServiceDaoSpecContext {
    val bucketName = Config.s3ImagesBucket

    lazy val imageMockBucket = mock[Bucket].taggedWith[S3ImagesBucket]
    imageMockBucket.name returns bucketName

    val mockObjectSummary = mock[S3ObjectSummary]

    lazy val mockAWSS3Client = mock[AWSS3Client]
    private val mockListing = mock[ObjectListing]
    mockListing.getObjectSummaries.returns(List(mockObjectSummary).asJava)
    mockAWSS3Client.listObjects(bucketName) returns mockListing

    val s3Client = new S3Client {
      override lazy val s3Client = mockAWSS3Client
    }

    val service = new BarcodeService(s3Client, imageMockBucket)

    def buildExpectedKey(metadata: BarcodeMetadata): String = {
      val encodedValue = UUID.nameUUIDFromBytes(metadata.value.getBytes)
      val filename = s"$encodedValue.${metadata.width}x${metadata.height}.m${metadata.margin}"
      s"${merchant.id}/barcodes/${metadata.format}/$filename.png"
    }

  }

  "BarcodeService" should {

    "generate a new barcode" in new BarcodeServiceSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val metadata = random[BarcodeMetadata]

      val expectedBaseKey = buildExpectedKey(metadata)

      mockObjectSummary.getKey returns "another-key"

      val url = service.generate(metadata).await
      url ==== s"https://s3.amazonaws.com/$bucketName/$expectedBaseKey"

      there was one(imageMockBucket).putAsPublicRead(===(expectedBaseKey), any)(===(mockAWSS3Client))
    }

    "detect and reuse already generated barcode" in new BarcodeServiceSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val metadata = random[BarcodeMetadata]

      val expectedBaseKey = buildExpectedKey(metadata)

      mockObjectSummary.getKey returns expectedBaseKey

      val url = service.generate(metadata).await
      url ==== s"https://s3.amazonaws.com/$bucketName/$expectedBaseKey"

      there was no(imageMockBucket).putAsPublicRead(any, any)(any)
    }
  }
}
