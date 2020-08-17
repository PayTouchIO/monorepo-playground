package io.paytouch.core.services

import cats.implicits._

import org.scalacheck._

import org.specs2.scalacheck.Parameters

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.utils._

@scala.annotation.nowarn("msg=Auto-application")
final class GiftCardPassServiceSuite extends PaytouchSuite with FixtureRandomGenerators {
  "GiftCardPassService.generateOnlineCode" should {
    "generate codes which" in {
      "are 16 characters long" in {
        prop { onlineCode: GiftCardPass.OnlineCode => onlineCode.value.size ==== 16 }
      }

      "are alphanumeric with only upper case characters" in {
        prop { onlineCode: GiftCardPass.OnlineCode => onlineCode.value.forall(c => c.isDigit || c.isUpper) }
      }

      "are 19 characters long when hyphenated because they will contain only 3 hyphens in the correct positions" in {
        prop { onlineCode: GiftCardPass.OnlineCode =>
          val hyphenatedValue = onlineCode.hyphenated.value

          hyphenatedValue.size ==== 19
          hyphenatedValue.count(_ === '-') ==== 3
          (4 to 14 by 5).map(hyphenatedValue.charAt).forall(_ ==== '-')
        }
      }

      "can be isomorphic (roundtrip) to their hyphenated versions" in {
        prop { onlineCode: GiftCardPass.OnlineCode => onlineCode.hyphenated.hyphenless ==== onlineCode }
      }

      "be random enough to make this test theoretically not flaky" in {
        prop { onlineCodes: List[GiftCardPass.OnlineCode] => onlineCodes.distinct.size ==== onlineCodes.size }
        // This test might fail at some point in the future when the random generator hits the lottery.
        // After it does for the first time we should delete this comment and uncomment the following line:
        // .set(maxDiscardRatio = 1)
      }
    }
  }
}
