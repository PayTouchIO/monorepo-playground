package io.paytouch.core.resources.imageuploads

import akka.http.scaladsl.model.StatusCodes
import cats.implicits._

import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.entities.{ ImageUpload => ImageUploadEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ImageUploadsGetFSpec extends ImageUploadFSpec {

  abstract class ImageUploadResourceFSpecContext extends ImageUploadFSpecContext {
    def assertResponse(record: ImageUploadRecord, entity: ImageUploadEntity) = {
      record.id ==== entity.id
      record.urls ==== entity.urls
      record.fileName ==== entity.fileName
      record.objectId ==== entity.objectId
      record.objectType ==== entity.objectType
    }
  }

  "GET /v1/image_uploads.get?image_upload_id=$" in {
    "if request has valid token" in {

      "if the image upload belongs to the merchant" should {
        "return an image upload" in new ImageUploadResourceFSpecContext {
          val urls = Map(
            "default" -> "http://s3.amazonaws.com/my-test-bucket/foo/bar/baz",
          )
          val imageUpload = Factory.imageUpload(merchant, urls = urls.some).create

          Get(s"/v1/image_uploads.get?image_upload_id=${imageUpload.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val importResponse = responseAs[ApiResponse[ImageUploadEntity]]
            val expectedUrls = Map(
              "default" -> "http://my-test-cloudfront/foo/bar/baz",
            )
            val expectedImageUpload = imageUpload.copy(urls = expectedUrls.some)
            assertResponse(expectedImageUpload, importResponse.data)
          }
        }
      }

      "if the import does not belong to the merchant" should {
        "return 404" in new ImageUploadResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorImageUpload = Factory.imageUpload(competitor).create

          Get(s"/v1/image_uploads.get?image_upload_id=${competitorImageUpload.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
