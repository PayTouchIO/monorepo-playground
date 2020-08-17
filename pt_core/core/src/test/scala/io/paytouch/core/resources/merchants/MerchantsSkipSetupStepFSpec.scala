package io.paytouch.core.resources.merchants

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class MerchantsSkipSetupStepFSpec extends MerchantsFSpec {

  abstract class MerchantsSkipSetupStepFSpecContext extends MerchantResourceFSpecContext {
    val merchantDao = daos.merchantDao
    val randomStep = random[MerchantSetupSteps]
    val randomStepParam = randomStep.entryName

    def assertStepIsSkipped() = {
      val reloadedMerchant = merchantDao.findById(merchant.id).await.get
      val setupSteps = reloadedMerchant.setupSteps
      setupSteps.flatMap(_.get(randomStep)) must beSome
    }
  }

  "POST /v1/merchants.skip_setup_step?step=$" in {
    "if request has valid token" in {
      "merchant id is the current merchant's id" should {
        "update step and return 204" in new MerchantsSkipSetupStepFSpecContext {
          Post(s"/v1/merchants.skip_setup_step?step=$randomStepParam")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertStepIsSkipped()
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new MerchantsSkipSetupStepFSpecContext {
        Post(s"/v1/merchants.skip_setup_step?step=$randomStepParam")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
