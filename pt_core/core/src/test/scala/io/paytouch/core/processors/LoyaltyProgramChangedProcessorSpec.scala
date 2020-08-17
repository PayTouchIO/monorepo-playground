package io.paytouch.core.processors

import scala.concurrent._

import com.softwaremill.macwire._

import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.LoyaltyProgram
import io.paytouch.core.messages.entities.LoyaltyProgramChanged
import io.paytouch.core.services.UrbanAirshipService
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class LoyaltyProgramChangedProcessorSpec extends ProcessorSpec {
  abstract class LoyaltyProgramChangedProcessorSpecContext extends ProcessorSpecContext with StateFixtures {
    val loyaltyProgramService = MockedRestApi.loyaltyProgramService
    val templateData = random[TemplateData.LoyaltyTemplateData]
    val urbanAirshipService = mock[UrbanAirshipService]

    lazy val processor = wire[LoyaltyProgramChangedProcessor]

    urbanAirshipService.prepareLoyaltyTemplateData(any, any) returns Future.successful(Some(templateData))

    val loyaltyProgramDao = daos.loyaltyProgramDao
  }

  "LoyaltyProgramProcessor" in {

    "if record is found" should {
      "if appleWalletTemplateId is None" should {
        "create template for iOS" in new LoyaltyProgramChangedProcessorSpecContext {
          val loyaltyProgram = Factory
            .loyaltyProgram(merchant, appleWalletTemplateId = None, androidPayTemplateId = Some("androidFoo"))
            .create
          val entity = random[LoyaltyProgram].copy(id = loyaltyProgram.id)

          urbanAirshipService.upsertTemplate[TemplateData.LoyaltyTemplateData](any, any, any)(any, any) returns Future
            .successful(
              templateUpserted,
            )

          processor.execute(LoyaltyProgramChanged(merchant.id, entity))

          afterAWhile {
            there was one(urbanAirshipService).upsertTemplate(loyaltyProgram.id, PassType.Ios, templateData)

            val loyaltyProgramUpdated = loyaltyProgramDao.findById(loyaltyProgram.id).await.get
            loyaltyProgramUpdated.androidPayTemplateId ==== Some("androidFoo")
            loyaltyProgramUpdated.appleWalletTemplateId ==== Some(templateUpserted.templateId)
          }
        }
      }

      "if androidPayTemplateId is None" should {
        "create template for iOS" in new LoyaltyProgramChangedProcessorSpecContext {
          val loyaltyProgram = Factory
            .loyaltyProgram(merchant, appleWalletTemplateId = Some("iosFoo"), androidPayTemplateId = None)
            .create
          val entity = random[LoyaltyProgram].copy(id = loyaltyProgram.id)

          urbanAirshipService.upsertTemplate[TemplateData.LoyaltyTemplateData](any, any, any)(any, any) returns Future
            .successful(
              templateUpserted,
            )

          processor.execute(LoyaltyProgramChanged(merchant.id, entity))

          afterAWhile {
            there was one(urbanAirshipService).upsertTemplate(loyaltyProgram.id, PassType.Ios, templateData)

            val loyaltyProgramUpdated = loyaltyProgramDao.findById(loyaltyProgram.id).await.get
            loyaltyProgramUpdated.androidPayTemplateId ==== Some(templateUpserted.templateId)
            loyaltyProgramUpdated.appleWalletTemplateId ==== Some("iosFoo")
          }
        }
      }

    }

    "if record is not found" in new LoyaltyProgramChangedProcessorSpecContext {
      val entity = random[LoyaltyProgram]

      processor.execute(LoyaltyProgramChanged(merchant.id, entity))

      afterAWhile {
        there was noCallsTo(urbanAirshipService)
      }
    }
  }

  trait StateFixtures extends MultipleLocationFixtures {
    val templateUpserted = random[TemplateUpserted]
  }
}
