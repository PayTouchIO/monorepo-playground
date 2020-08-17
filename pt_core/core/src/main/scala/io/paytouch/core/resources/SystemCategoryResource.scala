package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.expansions.CategoryExpansions
import io.paytouch.core.filters.CategoryFilters
import io.paytouch.core.services.SystemCategoryService

trait SystemCategoryResource extends ProductCategoryResource {

  def systemCategoryService: SystemCategoryService

  val systemCategoryRoutes: Route =
    path("categories.assign_products") {
      post {
        parameter("category_id".as[UUID]) { id =>
          entity(as[ProductsAssignment]) { productsAssignment =>
            authenticate { implicit user =>
              onSuccess(systemCategoryService.assignProducts(id, productsAssignment)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
    } ~
      path("categories.create") {
        post {
          parameters("category_id".as[UUID]) { id =>
            entity(as[SystemCategoryCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(systemCategoryService.create(id, creation))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("categories.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(systemCategoryService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("categories.get") {
        get {
          parameter("category_id".as[UUID]) { id =>
            authenticate { implicit user =>
              expandParameters("subcategories", "locations", "products_count", "availabilities")(
                CategoryExpansions.apply,
              ) { expansions =>
                onSuccess(systemCategoryService.findById(id)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("categories.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters("location_id".as[UUID].?, "q".?, "updated_since".as[ZonedDateTime].?)
              .as(CategoryFilters.forSystemCategories _) { filters =>
                expandParameters("subcategories", "locations", "products_count", "availabilities")(
                  CategoryExpansions.apply,
                ) { expansions =>
                  userOrAppAuthenticate { implicit user =>
                    onSuccess(systemCategoryService.findAll(filters)(expansions)) {
                      case result =>
                        completeAsPaginatedApiResponse(result)
                    }
                  }
                }
              }
          }
        }
      } ~
      path("categories.update") {
        post {
          parameter("category_id".as[UUID]) { id =>
            entity(as[SystemCategoryUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(systemCategoryService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("categories.update_active") {
        parameters("category_id".as[UUID]) { id =>
          post {
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(systemCategoryService.updateActiveLocations(id, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("categories.update_ordering") {
        post {
          entity(as[Seq[EntityOrdering]]) { ordering =>
            authenticate { implicit user =>
              onSuccess(systemCategoryService.updateOrdering(ordering))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      updateProductsOrdering("categories", "category_id")
}
