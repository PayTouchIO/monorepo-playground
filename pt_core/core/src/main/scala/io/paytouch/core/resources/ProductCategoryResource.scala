package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.EntityOrdering
import io.paytouch.core.services.ProductCategoryService

trait ProductCategoryResource extends JsonResource {

  def productCategoryService: ProductCategoryService

  protected def updateProductsOrdering(pathVerb: String, paramName: String): Route =
    path(s"$pathVerb.update_products_ordering") {
      post {
        parameter(s"$paramName".as[UUID]) { categoryId =>
          entity(as[Seq[EntityOrdering]]) { ordering =>
            authenticate { implicit user =>
              onSuccess(productCategoryService.updateOrdering(categoryId, ordering)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
    }
}
