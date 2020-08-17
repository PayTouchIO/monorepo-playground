package io.paytouch.core.processors

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.entities.GiftCardPass
import io.paytouch.core.messages.entities.GiftCardPassChanged
import io.paytouch.core.services.GiftCardPassService
import io.paytouch.core.utils.MultipleLocationFixtures

import scala.concurrent._
import org.specs2.concurrent.ExecutionEnv

class GiftCardPassChangedProcessorSpec extends ProcessorSpec {

  abstract class GiftCardPassChangedProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    implicit val userCtx = userContext
    val giftCardPassService = mock[GiftCardPassService]

    val processor = wire[GiftCardPassChangedProcessor]
    val giftCardPassId = UUID.randomUUID
    val giftCardPass = random[GiftCardPass]

    def assertUpsertPassCalled() =
      there was one(giftCardPassService).upsertPass(giftCardPass)(userCtx)
  }

  "GiftCardPassChangedProcessor" in {
    "execute" in {
      "if upsertPass is successful" in {
        "return success" in new GiftCardPassChangedProcessorSpecContext {
          giftCardPassService.upsertPass(any)(any) returns Future.successful(None)

          val cardStatusChangedMessage = GiftCardPassChanged(giftCardPass)
          processor.execute(cardStatusChangedMessage).await

          assertUpsertPassCalled()
        }
      }

      "if upsertPass can't find a template" should {
        "return success" in new GiftCardPassChangedProcessorSpecContext {
          giftCardPassService.upsertPass(any)(any) returns Future.successful(Some(giftCardPass))

          val cardStatusChangedMessage = GiftCardPassChanged(giftCardPass)
          processor.execute(cardStatusChangedMessage).await

          assertUpsertPassCalled()
        }
      }

      "if upsertPass throws an error" should {
        "return failure" in new GiftCardPassChangedProcessorSpecContext {
          implicit val ee = ExecutionEnv.fromGlobalExecutionContext

          giftCardPassService.upsertPass(any)(any) returns Future.failed(new RuntimeException("Error"))

          val cardStatusChangedMessage = GiftCardPassChanged(giftCardPass)
          processor.execute(cardStatusChangedMessage) must throwAn[Exception].await

          assertUpsertPassCalled()
        }
      }
    }
  }
}
