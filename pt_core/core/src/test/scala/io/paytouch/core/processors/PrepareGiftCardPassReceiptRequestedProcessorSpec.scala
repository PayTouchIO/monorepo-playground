package io.paytouch.core.processors

import scala.concurrent._

import io.paytouch.core.entities._
import io.paytouch.core.messages.entities.PrepareGiftCardPassReceiptRequested
import io.paytouch.core.services.GiftCardPassService
import io.paytouch.core.utils.MultipleLocationFixtures

class PrepareGiftCardPassReceiptRequestedProcessorSpec extends ProcessorSpec {
  abstract class PrepareGiftCardPassReceiptRequestedProcessorSpecContext
      extends ProcessorSpecContext
         with MultipleLocationFixtures {
    implicit val u: UserContext = userContext

    val giftCardPassServiceMock = mock[GiftCardPassService]

    lazy val processor = new PrepareGiftCardPassReceiptRequestedProcessor(giftCardPassServiceMock)
  }

  "PrepareGiftCardPassReceiptRequestedProcessor" in {

    "trigger compute reports for the order" in new PrepareGiftCardPassReceiptRequestedProcessorSpecContext {
      val giftCardPass = random[GiftCardPass]
      giftCardPassServiceMock.upsertPass(any)(any) returns Future.successful(Some(giftCardPass))

      processor.execute(PrepareGiftCardPassReceiptRequested(giftCardPass))

      afterAWhile {
        there was one(giftCardPassServiceMock).sendGiftCardPassReceiptMsg(giftCardPass)
      }
    }
  }
}
