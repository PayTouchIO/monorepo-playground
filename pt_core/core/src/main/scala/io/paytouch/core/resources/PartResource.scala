package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.services.PartService

trait PartResource extends JsonResource {

  def partService: PartService

  protected lazy val partSpecificRoutes: Route =
    path("parts.create") {
      post {
        parameters("part_id".as[UUID]) { partId =>
          entity(as[PartCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(partService.create(partId, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("parts.update") {
        post {
          parameters("part_id".as[UUID]) { partId =>
            entity(as[PartUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(partService.update(partId, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("recipes.create") {
        post {
          parameters("recipe_id".as[UUID]) { recipeId =>
            entity(as[RecipeCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(partService.create(recipeId, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("recipes.update") {
        post {
          parameters("recipe_id".as[UUID]) { recipeId =>
            entity(as[RecipeUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(partService.update(recipeId, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
