package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.{ BrandCreation, BrandUpdate }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.services.BrandService

trait BrandResource extends JsonResource {

  def brandService: BrandService

  val brandRoutes: Route =
    path("brands.create") {
      post {
        parameters("brand_id".as[UUID]) { id =>
          entity(as[BrandCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(brandService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("brands.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            authenticate { implicit user =>
              onSuccess(brandService.findAll(NoFilters())(NoExpansions())) {
                case result =>
                  completeAsPaginatedApiResponse(result)
              }
            }
          }
        }
      } ~
      path("brands.update") {
        post {
          parameter("brand_id".as[UUID]) { id =>
            entity(as[BrandUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(brandService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      }
}
