package io.paytouch.ordering.resources.ekashu

import io.paytouch.ordering.data.model.EkashuConfig
import io.paytouch.ordering.ekashu.EkashuEncodings
import io.paytouch.ordering.utils.PaytouchSpec

class EkashuEncodingsSpec extends PaytouchSpec with EkashuEncodings {

  "getPaymentProcessorConfigashuHashCode" should {
    "return the expected value" in {
      val hashKey = "trVxrnoz22bvwvnV"
      val terminalId = "99999999"
      val reference = "0000000765"
      val amount = "1.23"
      val expected = "7PtU022473m+ntcZY2wt6pXzKWc="

      implicit val e: EkashuConfig = EkashuConfig(
        hashKey = hashKey,
        sellerId = terminalId,
        sellerKey = "unrelevant",
      )

      calculateEkashuHashCode(reference, amount) ==== expected
    }
  }

  "calculateEkashuHashSuccessResult" should {
    "return the expected value" in {
      val hashKey = "trVxrnoz22bvwvnV"
      val terminalId = "99999999"
      val transactionId = "85761ABA-5415-DE11-9A1E-000F1F660B7C"

      val expected = "1XK8gUyv/TfH8fBPpWg8UK0jmYo="

      implicit val e: EkashuConfig = EkashuConfig(
        hashKey = hashKey,
        sellerId = terminalId,
        sellerKey = "unrelevant",
      )

      calculateEkashuHashSuccessResult(transactionId) ==== expected
    }
  }

  "calculateEkashuHashFailureResult" should {
    "return the expected value" in {
      val hashKey = "trVxrnoz22bvwvnV"
      val terminalId = "99999999"
      val transactionId = "85761ABA-5415-DE11-9A1E-000F1F660B7C"

      val expected = "D5nFs6SetR/CZiEwP0JTQlVZddU="

      implicit val e: EkashuConfig = EkashuConfig(
        hashKey = hashKey,
        sellerId = terminalId,
        sellerKey = "unrelevant",
      )

      calculateEkashuHashFailureResult(transactionId) ==== expected
    }
  }
}
