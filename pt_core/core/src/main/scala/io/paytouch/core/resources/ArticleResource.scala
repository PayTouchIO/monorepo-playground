package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleTypeAlias }
import io.paytouch.core.entities._
import io.paytouch.core.expansions.ArticleExpansions
import io.paytouch.core.filters.ArticleFilters
import io.paytouch.core.services.ArticleService

trait ArticleResource extends JsonResource {
  self: InventoryResource with PartResource with ProductHistoryResource with ProductPartResource with ProductResource =>

  def articleService: ArticleService

  lazy val productRoutes: Route =
    genericRoutes ~
      inventoryRoutes ~
      partSpecificRoutes ~
      productPartRoutes ~
      productHistoryRoutes ~
      productSpecificRoutes

  private lazy val genericRoutes: Route =
    path("articles.list") {
      parameter("scope".as[ArticleScope].?)(scope => articlesList(scope))
    } ~
      path("products.assign_modifier_sets") {
        post {
          parameter("product_id".as[UUID]) { productId =>
            entity(as[ProductModifierSetsAssignment]) { modifierSetsAssignment =>
              authenticate { implicit user =>
                onSuccess(articleService.assignModifierSets(productId, modifierSetsAssignment)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("products.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(articleService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("products.get") {
        get {
          parameter("product_id".as[UUID]) { productId =>
            expandParameters(
              "categories",
              "category_ids",
              "variants",
              "modifiers",
              "modifier_ids",
              "modifier_positions",
              "tax_rates",
              "tax_rate_locations",
              "tax_rate_ids",
              "stock_level",
              "suppliers",
              "recipe_details",
              "category_positions",
              "reorder_amount",
              "price_ranges",
              "cost_ranges",
              "catalog_categories",
              "catalog_category_positions",
              "catalog_category_options",
            )(ArticleExpansions.apply) { expansions =>
              userOrAppAuthenticate { implicit user =>
                onSuccess(articleService.findById(productId)(expansions))(result => completeAsOptApiResponse(result))
              }
            }
          }
        }
      } ~
      path("products.list") {
        parameter("scope".as[ArticleScope].?(ArticleScope.Product: ArticleScope))(scope => articlesList(Some(scope)))
      } ~
      path("products.update_active") {
        post {
          parameters("product_id".as[UUID]) { productId =>
            entity(as[Seq[UpdateActiveLocation]]) { updateActiveLocations =>
              authenticate { implicit user =>
                onSuccess(articleService.updateActiveLocations(productId, updateActiveLocations)) { result =>
                  completeAsEmptyResponse(result)
                }
              }
            }
          }
        }
      }

  private def articlesList(scope: Option[ArticleScope]): Route =
    get {
      paginateWithDefaults(30) { implicit pagination =>
        parameters(
          "category_id".as[UUID].?,
          "category_id[]".as[Seq[UUID]].?,
          "location_id".as[UUID].?,
          "location_id[]".as[Seq[UUID]].?,
          "modifier_set_id".as[UUID].?,
          "supplier_id".as[UUID].?,
          "loyalty_reward_id".as[UUID].?,
          "q".?,
          "updated_since".as[ZonedDateTime].?,
          "low_inventory".as[Boolean].?,
          "is_combo".as[Boolean].?,
          "type".as[ArticleTypeAlias].?,
          "type[]".as[Seq[ArticleTypeAlias]].?,
          "catalog_category_id[]".as[Seq[UUID]].?,
          "ids[]".as[Seq[UUID]].?,
          "catalog_id[]".as[Seq[CatalogId]].?,
        ) {
          (
              systemCategoryId,
              systemCategoryIds,
              locationId,
              locationIds,
              modifierSetId,
              supplierId,
              loyaltyRewardId,
              query,
              updatedSince,
              lowInventory,
              isCombo,
              typeAlias,
              typeAliases,
              catalogCategoryIds,
              ids,
              catalogIds,
          ) =>
            expandParameters(
              "categories",
              "category_ids",
              "variants",
              "modifiers",
              "modifier_ids",
              "modifier_positions",
              "tax_rates",
              "tax_rate_locations",
              "tax_rate_ids",
              "stock_level",
              "suppliers",
              "recipe_details",
              "category_positions",
              "reorder_amount",
              "price_ranges",
              "cost_ranges",
              "catalog_categories",
              "catalog_category_positions",
              "catalog_category_options",
            )(ArticleExpansions.apply) { expansions =>
              userOrAppAuthenticate { implicit user =>
                val filters = ArticleFilters.forList(
                  systemCategoryId,
                  systemCategoryIds,
                  locationIds.combineWithOne(locationId),
                  modifierSetId,
                  supplierId,
                  loyaltyRewardId,
                  query,
                  updatedSince,
                  lowInventory,
                  isCombo,
                  scope,
                  typeAlias,
                  typeAliases,
                  catalogCategoryIds,
                  ids,
                  catalogIds.map(_.map(_.cast.get)),
                )

                onSuccess(articleService.findAll(filters)(expansions)) { (products, count) =>
                  completeAsPaginatedApiResponse(products, count)
                }
              }
            }
        }
      }
    }
}
