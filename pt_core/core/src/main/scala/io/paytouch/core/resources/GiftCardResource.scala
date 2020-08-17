package io.paytouch.core.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.GiftCardFilters
import io.paytouch.core.services.GiftCardService

trait GiftCardResource extends JsonResource {

  def giftCardService: GiftCardService

  val giftCardRoutes: Route =
    path("gift_cards.create") {
      post {
        parameters("gift_card_id".as[UUID]) { id =>
          entity(as[GiftCardCreation]) { creation =>
            authenticate { implicit user =>
              onSuccess(giftCardService.create(id, creation))(result => completeAsApiResponse(result))
            }
          }
        }
      }
    } ~
      path("gift_cards.delete") {
        post {
          entity(as[Ids]) { deletion =>
            authenticate { implicit user =>
              onSuccess(giftCardService.bulkDelete(deletion.ids))(result => completeAsEmptyResponse(result))
            }
          }
        }
      } ~
      path("gift_cards.get") {
        get {
          parameter("gift_card_id".as[UUID]) { id =>
            authenticate { implicit user =>
              onSuccess(giftCardService.findById(id)(NoExpansions()))(result => completeAsOptApiResponse(result))
            }
          }
        }
      } ~
      path("gift_cards.list") {
        get {
          paginateWithDefaults(30) { implicit pagination =>
            userOrAppAuthenticate { implicit user =>
              parameters("updated_since".as[ZonedDateTime].?).as(GiftCardFilters) { filters =>
                onSuccess(giftCardService.findAll(filters)(NoExpansions())) {
                  case result =>
                    completeAsPaginatedApiResponse(result)
                }
              }
            }
          }
        }
      } ~
      path("gift_cards.update") {
        post {
          parameter("gift_card_id".as[UUID]) { id =>
            entity(as[GiftCardUpdate]) { update =>
              authenticate { implicit user =>
                onSuccess(giftCardService.update(id, update))(result => completeAsApiResponse(result))
              }
            }
          }
        }
      } ~
      path("gift_cards.update_active") {
        post {
          entity(as[Seq[UpdateActiveItem]]) { updateActiveItems =>
            authenticate { implicit user =>
              onSuccess(giftCardService.updateActiveItems(updateActiveItems)) { result =>
                completeAsEmptyResponse(result)
              }
            }
          }
        }
      }
}
