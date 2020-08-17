package io.paytouch.core.resources.imageuploads

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.RouteTestTimeout
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.{ ServiceConfigurations => Config }

import scala.concurrent.duration._

abstract class GenericImageUploadCreateFSpec extends ImageUploadFSpec {

  override implicit val timeout = RouteTestTimeout(2 minutes)

  abstract class GenericImageUploadCreateFSpecContext extends ImageUploadFormResourceFSpecContext {
    val s3ImagesBucket = Config.s3ImagesBucket
  }

  def assertImageUploadCreation(
      imageType: String,
      bucketName: String,
      sizeFilenameMap: Map[String, String],
    ) =
    s"POST /v1/image_uploads.create (type = $imageType)" in {
      "if request has valid token" in {

        "if the provided id is valid" should {
          "upload the resized images to S3 and update the image upload" in new GenericImageUploadCreateFSpecContext {
            val imageUploadId = UUID.randomUUID
            val baseKey = s"${merchant.id}/$bucketName/$imageUploadId"

            MultiformDataRequest(
              s"/v1/image_uploads.create?image_upload_id=$imageUploadId&type=$imageType",
              ValidImage,
              "img",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              afterAWhile {
                val imageUploadRecord = imageUploadDao.findById(imageUploadId).await.get
                imageUploadRecord.urls.isDefined should beTrue
                val urls = imageUploadRecord.urls.get
                sizeFilenameMap.foreach {
                  case (size, filename) =>
                    urls.get(size) ==== Some(s"https://s3.amazonaws.com/$s3ImagesBucket/$baseKey/$filename.png")
                    there was one(mockBucket).putAsPublicRead(===(s"$baseKey/$filename.png"), any)(===(mockAWSS3Client))
                }
                ok
              }
            }
          }

          "reject a file that is not an image" in new GenericImageUploadCreateFSpecContext {
            val imageUploadId = UUID.randomUUID

            MultiformDataRequest(
              s"/v1/image_uploads.create?image_upload_id=$imageUploadId&type=$imageType",
              InvalidImage,
              "img",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              imageUploadDao.findById(imageUploadId).await ==== None
            }
          }
        }

        "if the selected id is already taken by another merchant" should {
          "return 404" in new GenericImageUploadCreateFSpecContext {
            val competitor = Factory.merchant.create
            val competitorImageUpload = Factory.imageUpload(competitor).create

            MultiformDataRequest(
              s"/v1/image_uploads.create?image_upload_id=${competitorImageUpload.id}&type=$imageType",
              ValidImage,
              "img",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
}
