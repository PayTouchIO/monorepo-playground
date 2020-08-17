package io.paytouch.core.resources.imageuploads.v2

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import cats.implicits._

import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ ImageUpload => ImageUploadEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities.enums.RegularImageSize
import java.net.URLEncoder

class ImageUploadUploadCompleteFSpec extends ImageUploadFSpec {
  abstract class Context extends ImageUploadFSpecContext {
    def assertResponse(
        record: ImageUploadRecord,
        response: ImageUploadEntity,
        urls: Option[Map[String, String]],
      ): Unit = {
      response.id ==== record.id
      response.fileName ==== record.fileName
      response.objectId ==== record.objectId
      response.objectType ==== record.objectType
      response.urls ==== urls
      response.uploadUrl must beNone
    }

    def assertUpdate(original: ImageUploadRecord, urls: Option[Map[String, String]]): Unit = {
      val record = imageUploadDao.findById(original.id).await.get
      record.objectType ==== original.objectType
      record.objectId ==== original.objectId
      record.fileName ==== original.fileName
      record.urls ==== urls
    }
  }

  "POST /v2/image_uploads.upload_complete" in {
    "if request has valid token" in {
      "if the image is newly uploaded" in {
        "return the image upload with cloudinary urls" in new Context {
          val imageUpload = Factory
            .imageUpload(
              merchant,
              objectId = Some(UUID.randomUUID),
              imageUploadType = Some(ImageUploadType.Product),
              urls = None,
            )
            .create

          Post(s"/v2/image_uploads.upload_complete?image_upload_id=${imageUpload.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val s3Url =
              s"https://s3.amazonaws.com/my-test-bucket/${imageUpload.merchantId}/${imageUpload.objectType.bucketName}/${imageUpload.id}/${imageUpload.fileName}"
            val s3UrlParam = URLEncoder.encode(s3Url, "UTF-8")

            val expectedUrls = RegularImageSize
              .values
              .map { size =>
                val url = s"https://my-test-cloudinary/image/fetch/${size.cloudinaryFormatString}/${s3UrlParam}"
                size.description -> url
              }
              .toMap

            val result = responseAs[ApiResponse[ImageUploadEntity]].data
            assertResponse(imageUpload, result, Some(expectedUrls))
            assertUpdate(imageUpload, Some(expectedUrls))
          }
        }
      }

      "if the image already has images urls assigned" in {
        "return the image without changing it" in new Context {
          val urls = Map(
            "small" -> "https://unsplash.com/photos/XIEFrTSXoOw/download?force=true&w=640",
            "medium" -> "https://unsplash.com/photos/XIEFrTSXoOw/download?force=true&w=1920",
            "large" -> "https://unsplash.com/photos/XIEFrTSXoOw/download?force=true&w=2400",
          )

          val imageUpload = Factory
            .imageUpload(
              merchant,
              objectId = Some(UUID.randomUUID),
              imageUploadType = Some(ImageUploadType.Product),
              urls = Some(urls),
            )
            .create

          Post(s"/v2/image_uploads.upload_complete?image_upload_id=${imageUpload.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ApiResponse[ImageUploadEntity]].data
            assertResponse(imageUpload, result, Some(urls))
            assertUpdate(imageUpload, Some(urls))
          }
        }
      }

      "if the image is a v1 upload" in {
        "return the image with cloudfront urls" in new Context {
          val urls = Map(
            "default" -> "http://s3.amazonaws.com/my-test-bucket/foo/bar/baz",
          )

          val imageUpload = Factory
            .imageUpload(
              merchant,
              objectId = Some(UUID.randomUUID),
              imageUploadType = Some(ImageUploadType.Product),
              urls = Some(urls),
            )
            .create

          Post(s"/v2/image_uploads.upload_complete?image_upload_id=${imageUpload.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val expectedUrls = Map(
              "default" -> "http://my-test-cloudfront/foo/bar/baz",
            )

            val result = responseAs[ApiResponse[ImageUploadEntity]].data
            assertResponse(imageUpload, result, Some(expectedUrls))
            assertUpdate(imageUpload, Some(urls))
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new Context {
        val id = UUID.randomUUID
        Post(s"/v2/image_uploads.upload_complete?image_upload_id=$id")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
