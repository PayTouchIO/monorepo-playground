package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.ProductPartFilters
import io.paytouch.core.services.ProductPartService

trait ProductPartResource extends JsonResource {

  def productPartService: ProductPartService

  lazy val productPartRoutes: Route =
    path("products.add_parts") {
      post {
        parameters("product_id".as[UUID]) { productId =>
          entity(as[Seq[ProductPartAssignment]]) { assignments =>
            authenticate { implicit user =>
              onSuccess(productPartService.assignProductParts(productId, assignments)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
    } ~
      path("products.list_parts") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("product_id".as[UUID]).as(ProductPartFilters) { filters =>
              authenticate { implicit user =>
                onSuccess(productPartService.findAll(filters)(NoExpansions())) { (productParts, count) =>
                  completeAsPaginatedApiResponse(productParts, count)
                }
              }
            }
          }
        }
      }
}
