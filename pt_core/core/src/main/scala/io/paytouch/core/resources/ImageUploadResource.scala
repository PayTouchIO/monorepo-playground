package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.services.ImageUploadService
import io.paytouch.core.entities.ImageUploadCreation

trait ImageUploadResource extends JsonResource {

  def imageUploadService: ImageUploadService

  val imageUploadRoutes: Route = concat(
    pathPrefix("v1") {
      path("image_uploads.get") {
        get {
          parameters("image_upload_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(imageUploadService.findById(id))(result => completeAsOptApiResponse(result))
            }
          }
        }
      }
    } ~ pathPrefix("v2") {
      concat(
        path("image_uploads.create") {
          post {
            parameters("image_upload_id".as[UUID]) { id =>
              entity(as[ImageUploadCreation]) { creation =>
                authenticate { implicit user =>
                  onSuccess(imageUploadService.create(id, creation))(result => completeAsApiResponse(result))
                }
              }
            }
          }
        } ~
          path("image_uploads.upload_complete") {
            post {
              parameters("image_upload_id".as[UUID]) { id =>
                authenticate { implicit user =>
                  onSuccess(imageUploadService.uploadComplete(id))(result => completeAsApiResponse(result))
                }
              }
            }
          },
      )
    },
  )
}
