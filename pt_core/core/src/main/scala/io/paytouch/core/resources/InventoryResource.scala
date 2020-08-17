package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleTypeAlias }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.{ InventoryFilters, ProductRevenueFilters }
import io.paytouch.core.services.InventoryService

trait InventoryResource extends JsonResource {
  def inventoryService: InventoryService

  val inventoryRoutes: Route =
    path("articles.list_inventory") {
      paginateWithDefaults(30) { implicit pagination =>
        parameters(
          "category_id".as[UUID].?,
          "category_id[]".as[Seq[UUID]].?,
          "location_id".as[UUID].?,
          "location_id[]".as[Seq[UUID]].?,
          "low".as[Boolean].?,
          "q".?,
          "supplier_id".as[UUID].?,
          "is_combo".as[Boolean].?,
          "scope".as[ArticleScope].?,
          "type".as[ArticleTypeAlias].?,
          "type[]".as[Seq[ArticleTypeAlias]].?,
        ) { (categoryId, categoryIds, locationId, locationIds, low, q, supplierId, isCombo, scope, `type`, types) =>
          val filters =
            InventoryFilters.forArticlesList(
              categoryId,
              categoryIds,
              locationIds.combineWithOne(locationId),
              low,
              q,
              supplierId,
              isCombo,
              scope,
              `type`,
              types,
            )

          get {
            authenticate { implicit user =>
              onSuccess(inventoryService.findAll(filters)(NoExpansions())) { (productInventories, count) =>
                completeAsPaginatedApiResponse(productInventories, count)
              }
            }
          }
        }
      }
    } ~
      path("products.list_inventory") {
        paginateWithDefaults(30) { implicit pagination =>
          parameters(
            "category_id".as[UUID].?,
            "category_id[]".as[Seq[UUID]].?,
            "location_id".as[UUID].?,
            "location_id[]".as[Seq[UUID]].?,
            "low".as[Boolean].?,
            "q".?,
            "supplier_id".as[UUID].?,
            "is_combo".as[Boolean].?,
          ) { (categoryId, categoryIds, locationId, locationIds, low, q, supplierId, isCombo) =>
            val filters =
              InventoryFilters.forProductsList(
                categoryId,
                categoryIds,
                locationIds.combineWithOne(locationId),
                low,
                q,
                supplierId,
                isCombo,
              )

            get {
              authenticate { implicit user =>
                onSuccess(inventoryService.findAll(filters)(NoExpansions())) { (productInventories, count) =>
                  completeAsPaginatedApiResponse(productInventories, count)
                }
              }
            }
          }
        }
      } ~
      path("products.compute_revenue") {
        parameters("product_id".as[UUID], "from".as[LocalDateTime].?, "to".as[LocalDateTime].?)
          .as(ProductRevenueFilters) { filters =>
            get {
              authenticate { implicit user =>
                onSuccess(inventoryService.computeRevenue(filters))(result => completeAsOptApiResponse(result))
              }
            }
          }
      }
}
