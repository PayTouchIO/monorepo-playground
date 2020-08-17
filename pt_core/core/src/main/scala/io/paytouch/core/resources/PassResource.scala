package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import io.paytouch.core.entities.enums._
import io.paytouch.core.services.PassService

trait PassResource extends JsonResource {
  def passService: PassService

  val passRoutes: Route = path("public" / "passes.install") {
    get {
      parameters(
        "id".as[UUID],
        "type".as[PassType],
        "item_type".as[PassItemType].?(PassItemType.LoyaltyMembership: PassItemType),
        "order_id".as[UUID].?,
      ) {
        case (itemId, passType, passItemType, orderId) =>
          authorize(passService.verifyUrl(_)) {
            onSuccess(passService.install(itemId, passType, passItemType, orderId)) {
              case Some(url) => redirect(url, StatusCodes.Found)
              case _         => complete(StatusCodes.NotFound)
            }
          }
      }
    }
  }
}
