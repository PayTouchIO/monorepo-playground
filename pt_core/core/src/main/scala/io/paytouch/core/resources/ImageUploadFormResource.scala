package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.ImageUploadUpsertionV1
import io.paytouch.core.services.ImageUploadService

trait ImageUploadFormResource extends FormDataResource {

  def imageUploadService: ImageUploadService

  val imageUploadFormRoutes: Route = path("image_uploads.create") {
    post {
      parameters(
        "image_upload_id".as[UUID],
        "type".as[ImageUploadType],
      ) { (id, imageUploadType) =>
        authenticate { implicit user =>
          saveImage(id) {
            case (fileInfo, file) =>
              val upsertion = ImageUploadUpsertionV1(imageUploadType, file, fileInfo.fileName)
              onSuccess(imageUploadService.scheduleImageUpload(id, upsertion)) { result =>
                completeValidatedData(result)
              }
          }
        }
      }
    }
  }
}
