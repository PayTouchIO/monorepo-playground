package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server._
import io.paytouch.core.entities.{ CommentCreation, CommentUpdate }
import io.paytouch.core.services.features.CommentableFeature

trait CommentResource extends JsonResource {

  def commentRoutes[T <: CommentableFeature](
      service: T,
      namespace: String,
      parameter: String,
    ): Route =
    path(s"$namespace.create_comment") {
      post {
        parameters("comment_id".as[UUID]) { id =>
          entity(as[CommentCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(service.createComment(id, creation.copy(objectType = None))) { result =>
                completeAsApiResponse(result)
              }
            }
          }
        }
      }
    } ~
      path(s"$namespace.delete_comment") {
        post {
          parameters("comment_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(service.deleteComment(id))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path(s"$namespace.list_comments") {
        get {
          parameters(Symbol(parameter).as[UUID]) { objectId: UUID =>
            paginateWithDefaults(30) { implicit pagination =>
              authenticate { implicit user =>
                onSuccess(service.listComments(objectId))(result => completeAsValidatedPaginatedApiResponse(result))
              }
            }
          }
        }
      } ~
      path(s"$namespace.update_comment") {
        post {
          parameters("comment_id".as[UUID]) { id =>
            entity(as[CommentUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(service.updateComment(id, update.copy(objectType = None))) { result =>
                  completeAsApiResponse(result)
                }
              }
            }
          }
        }
      }

}
