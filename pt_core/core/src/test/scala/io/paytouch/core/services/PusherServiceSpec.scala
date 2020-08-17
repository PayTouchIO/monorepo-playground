package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.{ PusherKey, PusherSecret }
import io.paytouch.core.data.model.enums.BusinessType
import io.paytouch.core.entities.{ PusherAuthentication, PusherToken, UserContext }
import io.paytouch.core.entities.enums.ContextSource
import io.paytouch.utils.Tagging._

class PusherServiceSpec extends ServiceDaoSpec {
  abstract class PusherServiceSpecContext extends ServiceDaoSpecContext {
    val service = new PusherService(key = "foo".taggedWith[PusherKey], secret = "bar".taggedWith[PusherSecret])

    val merchantId = UUID.nameUUIDFromBytes("merchant-id".getBytes)
    val locationId = UUID.nameUUIDFromBytes("location-id".getBytes)

    val validSocketId = "345.23"

    implicit val context =
      UserContext(
        id = UUID.randomUUID,
        merchantId = merchantId,
        currency = currency,
        businessType = BusinessType.Restaurant,
        locationIds = Seq(locationId),
        adminId = None,
        merchantSetupCompleted = true,
        source = ContextSource.PtDashboard,
        paymentProcessor = genPaymentProcessor.instance,
      )
  }

  "PusherService" in {
    "authenticate" should {
      "if channel contains merchant id and a location id" should {
        "return a PusherToken" in new PusherServiceSpecContext {
          val result = service
            .authenticate(
              PusherAuthentication(channelName = s"private-$merchantId-$locationId-channel", socketId = validSocketId),
            )
            .success

          result ==== PusherToken("foo:b70c1737409c405bafcd268faae159d7367a44de48704495ced4302b6bfed103")
        }
      }

      "if channel contains only merchant id" should {
        "return a Valid(PusherToken)" in new PusherServiceSpecContext {
          val result = service
            .authenticate(PusherAuthentication(channelName = s"private-$merchantId-channel", socketId = validSocketId))
            .success

          result ==== PusherToken("foo:195713867ffa2f0330e5ff7021dee06499115be099e74c361ca8b161567eeb4d")
        }
      }

      "if channel doesn't contain merchant id" should {
        "return an Invalid" in new PusherServiceSpecContext {
          service
            .authenticate(PusherAuthentication(channelName = s"private-channel", socketId = validSocketId))
            .failures
        }
      }
    }
  }
}
