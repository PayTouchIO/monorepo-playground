package io.paytouch.core.resources.merchants

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities.enums._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class MerchantsCompleteSetupStepFSpec extends MerchantsFSpec {

  abstract class MerchantsCompleteSetupStepFSpecContext extends MerchantResourceFSpecContext {
    val merchantDao = daos.merchantDao
    val randomStep = random[MerchantSetupSteps]
    val randomStepParam = randomStep.entryName

    def assertStepIsCompleted() = {
      val reloadedMerchant = merchantDao.findById(merchant.id).await.get
      val setupSteps = reloadedMerchant.setupSteps
      val setupStep = setupSteps.flatMap(_.get(randomStep)).get
      setupStep.completedAt must beSome
      setupStep.skippedAt must beNone
    }
  }

  "POST /v1/merchants.complete_setup_step?step=$" in {
    "if request has valid token" in {
      "merchant id is the current merchant's id" should {
        "update step and return 204" in new MerchantsCompleteSetupStepFSpecContext {
          Post(s"/v1/merchants.complete_setup_step?step=$randomStepParam")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertStepIsCompleted()
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new MerchantsCompleteSetupStepFSpecContext {
        Post(s"/v1/merchants.complete_setup_step?step=$randomStepParam")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
