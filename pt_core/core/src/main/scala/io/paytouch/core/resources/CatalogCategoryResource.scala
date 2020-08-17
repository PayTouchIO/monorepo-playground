package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.expansions.CategoryExpansions
import io.paytouch.core.filters.CategoryFilters
import io.paytouch.core.services.CategoryService

trait CatalogCategoryResource extends ProductCategoryResource {

  def categoryService: CategoryService

  val catalogCategoryRoutes: Route =
    path("catalog_categories.assign_products") {
      post {
        parameter("catalog_category_id".as[UUID]) { id =>
          entity(as[Seq[CatalogCategoryProductAssignment]]) { productsAssignment =>
            authenticate { implicit user =>
              onSuccess(categoryService.assignProducts(id, productsAssignment)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
    } ~
      path("catalog_categories.create") {
        post {
          parameters("catalog_category_id".as[UUID]) { id =>
            entity(as[CatalogCategoryCreation]) { creation =>
              authenticate { implicit user =>
                onSuccess(categoryService.create(id, CatalogCategoryCreation.convert(creation)))(result =>
                  completeAsApiResponse(result),
                )
              }
            }
          }
        }
      } ~
      path("catalog_categories.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(categoryService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("catalog_categories.get") {
        get {
          parameter("catalog_category_id".as[UUID]) { id =>
            authenticate { implicit user =>
              expandParameters("subcatalog_categories", "locations", "products_count", "availabilities")(
                CategoryExpansions.apply,
              ) { expansions =>
                onSuccess(categoryService.findById(id)(expansions)) { result =>
                  completeAsOptApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("catalog_categories.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "catalog_id".as[UUID],
              "location_id".as[UUID].?,
              "q".?,
              "updated_since".as[ZonedDateTime].?,
            ).as(CategoryFilters.forCatalogCategories _) { filters =>
              expandParameters("subcatalog_categories", "locations", "products_count", "availabilities")(
                CategoryExpansions.apply,
              ) { expansions =>
                userOrAppAuthenticate { implicit user =>
                  onSuccess(categoryService.findAll(filters)(expansions)) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
              }
            }
          }
        }
      } ~
      path("catalog_categories.update") {
        post {
          parameter("catalog_category_id".as[UUID]) { id =>
            entity(as[CatalogCategoryUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(categoryService.update(id, CatalogCategoryUpdate.convert(update)))(result =>
                  completeAsApiResponse(result),
                )
              }
            }
          }
        }
      } ~
      path("catalog_categories.update_active") {
        parameters("catalog_category_id".as[UUID]) { id =>
          post {
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(categoryService.updateActiveLocations(id, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("catalog_categories.update_ordering") {
        post {
          entity(as[Seq[EntityOrdering]]) { ordering =>
            authenticate { implicit user =>
              onSuccess(categoryService.updateOrdering(ordering))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      updateProductsOrdering("catalog_categories", "catalog_category_id")
}
