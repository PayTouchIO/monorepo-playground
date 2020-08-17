package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route

import cats.data._

import io.paytouch.core.entities.SendReceiptData
import io.paytouch.core.services.LoyaltyMembershipService

trait LoyaltyMembershipResource extends JsonResource {
  def loyaltyMembershipService: LoyaltyMembershipService

  val loyaltyMembershipRoutes: Route =
    path("loyalty_memberships.send_welcome_email") {
      post {
        parameter("loyalty_membership_id".as[UUID]) { id =>
          authenticate { implicit user =>
            onSuccess(loyaltyMembershipService.sendWelcomeEmail(id))(completeAsEmptyResponse)
          }
        }
      }
    }
}
