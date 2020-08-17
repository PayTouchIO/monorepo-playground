package io.paytouch.core.services

import com.softwaremill.macwire._
import io.paytouch.core.entities.enums.{ ExposedName, MerchantSetupSteps }
import io.paytouch.core.entities.{ ExposedEntity, SetupStepCondition, UserContext }
import io.paytouch.core.utils.{ MockedRestApi, FixtureDaoFactory => Factory }

final case class TestExposedEntity(foo: String) extends ExposedEntity {
  override def classShortName = ExposedName.Payroll
}

class SetupStepServiceSpec extends ServiceDaoSpec {

  abstract class SetupStepServiceSpecContext extends ServiceDaoSpecContext {
    lazy val merchantServiceSpy = spy(MockedRestApi.merchantService)
    val service = wire[SetupStepService]
    val entity = TestExposedEntity("randomString")
    val randomStep = random[MerchantSetupSteps]

    implicit val setupStepCondition: SetupStepCondition[TestExposedEntity] = _ => false

    def assertDoNothing() = there were noCallsTo(merchantServiceSpy)
    def assertStepMarkedAsCompleted()(implicit user: UserContext) =
      there was one(merchantServiceSpy).unvalidatedCompleteSetupStep(merchant.id, randomStep)
    def assertNoStepMarkedAsCompleted()(implicit user: UserContext) =
      there was no(merchantServiceSpy).unvalidatedCompleteSetupStep(merchant.id, randomStep)
  }

  "SetupStepService" in {
    "checkStepCompletion" in {
      "if merchantSetupCompleted=true" should {
        "do nothing" in new SetupStepServiceSpecContext {
          implicit val ctx = userCtx.copy(merchantSetupCompleted = true)
          service.checkStepCompletion(entity, randomStep).await
          assertDoNothing()
        }
      }
      "if merchantSetupCompleted=false" should {
        "if step is pending" should {
          "if step condition matches" should {
            "mark step as completed" in new SetupStepServiceSpecContext {
              implicit val ctx = userCtx.copy(merchantSetupCompleted = false)
              implicit val trueSetupStepCondition: SetupStepCondition[TestExposedEntity] = _ => true
              service.checkStepCompletion(entity, randomStep).await
              assertStepMarkedAsCompleted()
            }
          }
          "if step condition doesn't matches" should {
            "don't mark step as completed" in new SetupStepServiceSpecContext {
              implicit val ctx = userCtx.copy(merchantSetupCompleted = false)
              service.checkStepCompletion(entity, randomStep).await
              assertNoStepMarkedAsCompleted()
            }
          }
        }
        "if step is not pending" should {
          "do nothing" in new SetupStepServiceSpecContext {
            implicit val ctx = userCtx.copy(merchantSetupCompleted = false)
            merchantService.unvalidatedCompleteSetupStep(merchant.id, randomStep).await
            service.checkStepCompletion(entity, randomStep).await
            assertNoStepMarkedAsCompleted()
          }
        }
      }
    }
  }
}
