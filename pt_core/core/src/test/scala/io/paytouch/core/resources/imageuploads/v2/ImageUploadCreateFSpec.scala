package io.paytouch.core.resources.imageuploads.v2

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities.{ ImageUpload => ImageUploadEntity, _ }
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ImageUploadCreateFSpec extends ImageUploadFSpec {
  "POST /v2/image_uploads.create" in {
    "if request has valid token" in {
      "create an image upload with an upload url" in new ImageUploadFSpecContext {
        val id = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[ImageUploadCreation]

        Post(s"/v2/image_uploads.create?image_upload_id=$id", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val result = responseAs[ApiResponse[ImageUploadEntity]].data
          result.urls must beNone
          result.uploadUrl must beSome

          val record = imageUploadDao.findById(id).await.get
          record.objectType ==== creation.objectType
          record.fileName ==== creation.originalFileName
          record.objectId must beNone
          record.urls must beNone
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ImageUploadFSpecContext {
        val id = UUID.randomUUID

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[ImageUploadCreation]
        Post(s"/v2/image_uploads.create?image_upload_id=$id", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
