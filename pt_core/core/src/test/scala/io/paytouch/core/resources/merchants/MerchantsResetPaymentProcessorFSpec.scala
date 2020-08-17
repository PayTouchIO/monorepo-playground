package io.paytouch.core.resources.merchants

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.PaymentProcessor
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class MerchantsResetPaymentProcessorFSpec extends MerchantsFSpec {

  abstract class MerchantsResetPaymentProcessorFSpecContext extends MerchantResourceFSpecContext {
    val merchantDao = daos.merchantDao

    def assertPaymentProcessorIsReset() = {
      val reloadedMerchant = merchantDao.findById(merchant.id).await.get
      reloadedMerchant.paymentProcessor ==== PaymentProcessor.Paytouch
      reloadedMerchant.paymentProcessorConfig ==== PaymentProcessorConfig.Paytouch()
    }
  }

  "POST /v1/merchants.reset_payment_processor" in {
    "if request has valid token" in {
      "reset payment processor and return 204" in new MerchantsResetPaymentProcessorFSpecContext {
        Post(s"/v1/merchants.reset_payment_processor")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
          assertPaymentProcessorIsReset()
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new MerchantsResetPaymentProcessorFSpecContext {
        Post(s"/v1/merchants.reset_payment_processor")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
