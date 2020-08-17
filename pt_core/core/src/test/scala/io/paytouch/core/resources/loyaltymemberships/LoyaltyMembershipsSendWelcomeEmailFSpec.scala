package io.paytouch.core.resources.loyaltymemberships

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities.SendReceiptData
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class LoyaltyMembershipsSendWelcomeEmailFSpec extends LoyaltyMembershipsFSpec {
  abstract class LoyaltyMembershipsSendWelcomeEmailFSpecContext extends LoyaltyMembershipResourceFSpecContext {
    val globalCustomer =
      Factory
        .globalCustomerWithEmail(merchant = Some(merchant), email = Some(randomEmail))
        .create

    val loyaltyProgram =
      Factory
        .loyaltyProgram(
          merchant,
        )
        .create
  }

  "POST /v1/loyalty_memberships.send_welcome_email?loyalty_membership_id=$" in {
    "if request has valid token" in {
      "if the customer is enrolled into membership (happy path)" should {
        "send welcome email and return 204" in new LoyaltyMembershipsSendWelcomeEmailFSpecContext {
          val loyaltyMembership =
            Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now))
              .create

          Post(s"/v1/loyalty_memberships.send_welcome_email?loyalty_membership_id=${loyaltyMembership.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
          }
        }
      }

      "if the customer is NOT enrolled into membership" should {
        "NOT send welcome email and return 400" in new LoyaltyMembershipsSendWelcomeEmailFSpecContext {
          val loyaltyMembership =
            Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = None, customerOptInAt = None)
              .create

          Post(s"/v1/loyalty_memberships.send_welcome_email?loyalty_membership_id=${loyaltyMembership.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }

      "if loyaltyMembership not found" should {
        "NOT send welcome email and return 404" in new LoyaltyMembershipsSendWelcomeEmailFSpecContext {
          Post(s"/v1/loyalty_memberships.send_welcome_email?loyalty_membership_id=${UUID.randomUUID()}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
