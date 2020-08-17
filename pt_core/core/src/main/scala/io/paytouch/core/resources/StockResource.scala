package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.StockFilters
import io.paytouch.core.services.StockService

trait StockResource extends JsonResource { self: ArticleResource =>
  def stockService: StockService

  val stockRoutes: Route =
    path("stocks.bulk_create") {
      post {
        entity(as[Seq[StockCreation]]) { creations =>
          authenticate { implicit user =>
            onSuccess(stockService.bulkCreate(creations))(result => completeSeqAsApiResponse(result))
          }
        }
      }
    } ~
      path("stocks.bulk_update") {
        post {
          entity(as[Seq[StockUpdate]]) { updates =>
            authenticate { implicit user =>
              onSuccess(stockService.bulkUpdate(updates))(result => completeSeqAsApiResponse(result))
            }
          }
        }
      } ~
      path("stocks.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            parameters(
              "product_id".as[UUID].?,
              "location_id".as[UUID].?,
              "location_id[]".as[Seq[UUID]].?,
              "updated_since".as[ZonedDateTime].?,
            ) {
              (
                  productId,
                  locationId,
                  locationIds,
                  updatedSince,
              ) =>
                val filters =
                  StockFilters(
                    productId,
                    locationIds.combineWithOne(locationId),
                    updatedSince,
                  )

                authenticate { implicit user =>
                  onSuccess(stockService.findAll(filters)(NoExpansions())) {
                    case result =>
                      completeAsPaginatedApiResponse(result)
                  }
                }
            }
          }
        }
      }
}
