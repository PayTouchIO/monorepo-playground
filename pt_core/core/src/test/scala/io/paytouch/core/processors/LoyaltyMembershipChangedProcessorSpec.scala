package io.paytouch.core.processors

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.entities.LoyaltyMembership
import io.paytouch.core.messages.entities.LoyaltyMembershipChanged
import io.paytouch.core.services.LoyaltyMembershipService
import io.paytouch.core.utils.MultipleLocationFixtures

import scala.concurrent._

class LoyaltyMembershipChangedProcessorSpec extends ProcessorSpec {

  abstract class LoyaltyMembershipChangedProcessorSpecContext
      extends ProcessorSpecContext
         with MultipleLocationFixtures {
    implicit val userCtx = userContext

    val loyaltyMembershipService = mock[LoyaltyMembershipService]

    val processor = wire[LoyaltyMembershipChangedProcessor]
    val loyaltyMembershipId = UUID.randomUUID
    val loyaltyMembership = random[LoyaltyMembership]

    def assertUrbanAirshipPassUpserted() =
      there was one(loyaltyMembershipService).upsertPass(loyaltyMembership)
  }

  "LoyaltyMembershipChangedProcessor" in {
    "execute" in {
      "if update pass call fails" should {
        "attempt creation" in new LoyaltyMembershipChangedProcessorSpecContext {
          loyaltyMembershipService.upsertPass(any)(any) returns Future.successful(Some(loyaltyMembership))

          val loyaltyMembershipChangedMessage = LoyaltyMembershipChanged(loyaltyMembership)
          processor.execute(loyaltyMembershipChangedMessage)

          afterAWhile {
            assertUrbanAirshipPassUpserted()
          }
        }
      }
      "if active loyalty program doesn't exist" in {
        "update pass" in new LoyaltyMembershipChangedProcessorSpecContext {
          loyaltyMembershipService.upsertPass(any)(any) returns Future.successful(None)

          val loyaltyMembershipChangedMessage = LoyaltyMembershipChanged(loyaltyMembership)
          processor.execute(loyaltyMembershipChangedMessage)

          afterAWhile {
            assertUrbanAirshipPassUpserted()
          }
        }
      }
    }
  }
}
