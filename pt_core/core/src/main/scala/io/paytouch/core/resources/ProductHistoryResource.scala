package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.ProductHistoryFilters
import io.paytouch.core.services.{
  ProductCostHistoryService,
  ProductPriceHistoryService,
  ProductQuantityHistoryService,
}

trait ProductHistoryResource extends JsonResource {
  def productCostHistoryService: ProductCostHistoryService
  val productPriceHistoryService: ProductPriceHistoryService
  val productQuantityHistoryService: ProductQuantityHistoryService

  val productHistoryRoutes: Route =
    path("products.list_cost_changes") {
      paginateWithDefaults(30) { implicit pagination =>
        parameters("product_id".as[UUID], "from".as[LocalDateTime].?, "to".as[LocalDateTime].?)
          .as(ProductHistoryFilters) { filters =>
            get {
              authenticate { implicit user =>
                onSuccess(productCostHistoryService.findAll(filters)(NoExpansions())) { (items, count) =>
                  completeAsPaginatedApiResponse(items, count)
                }
              }
            }
          }
      }
    } ~
      path("products.list_price_changes") {
        paginateWithDefaults(30) { implicit pagination =>
          parameters("product_id".as[UUID], "from".as[LocalDateTime].?, "to".as[LocalDateTime].?)
            .as(ProductHistoryFilters) { filters =>
              get {
                authenticate { implicit user =>
                  onSuccess(productPriceHistoryService.findAll(filters)(NoExpansions())) { (items, count) =>
                    completeAsPaginatedApiResponse(items, count)
                  }
                }
              }
            }
        }
      } ~
      path("products.list_quantity_changes") {
        paginateWithDefaults(30) { implicit pagination =>
          parameters("product_id".as[UUID], "from".as[LocalDateTime].?, "to".as[LocalDateTime].?)
            .as(ProductHistoryFilters) { filters =>
              get {
                authenticate { implicit user =>
                  onSuccess(productQuantityHistoryService.findAll(filters)(NoExpansions())) { (items, count) =>
                    completeAsPaginatedApiResponse(items, count)
                  }
                }
              }
            }
        }
      }
}
