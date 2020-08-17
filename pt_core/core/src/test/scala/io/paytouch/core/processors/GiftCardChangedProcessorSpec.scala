package io.paytouch.core.processors

import scala.concurrent._

import com.softwaremill.macwire._

import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.GiftCard
import io.paytouch.core.messages.entities.GiftCardChanged
import io.paytouch.core.services.UrbanAirshipService
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class GiftCardChangedProcessorSpec extends ProcessorSpec {
  abstract class GiftCardChangedProcessorSpecContext extends ProcessorSpecContext with StateFixtures {
    val giftCardService = MockedRestApi.giftCardService
    val templateData = random[TemplateData.GiftCardTemplateData]
    val urbanAirshipService = mock[UrbanAirshipService]

    lazy val processor = wire[GiftCardChangedProcessor]

    urbanAirshipService.prepareGiftCardTemplateData(any, any) returns Future.successful(Some(templateData))

    val giftCardDao = daos.giftCardDao
  }

  "GiftCardChangedProcessor" in {

    "if record is found" should {
      "if appleWalletTemplateId is None" should {
        "create template for iOS" in new GiftCardChangedProcessorSpecContext {
          val giftCard = Factory
            .giftCard(giftCardProduct, appleWalletTemplateId = None, androidPayTemplateId = Some("androidFoo"))
            .create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[GiftCard].copy(id = giftCard.id)

          urbanAirshipService.upsertTemplate[TemplateData.GiftCardTemplateData](any, any, any)(any, any) returns Future
            .successful(
              templateUpserted,
            )

          processor.execute(GiftCardChanged(merchant.id, entity))

          afterAWhile {
            there was one(urbanAirshipService).upsertTemplate(giftCard.id, PassType.Ios, templateData)

            val giftCardUpdated = giftCardDao.findById(giftCard.id).await.get
            giftCardUpdated.androidPayTemplateId ==== Some("androidFoo")
            giftCardUpdated.appleWalletTemplateId ==== Some(templateUpserted.templateId)
          }
        }
      }

      "if androidPayTemplateId is None" should {
        "create template for iOS" in new GiftCardChangedProcessorSpecContext {
          val giftCard = Factory
            .giftCard(giftCardProduct, appleWalletTemplateId = Some("iosFoo"), androidPayTemplateId = None)
            .create

          @scala.annotation.nowarn("msg=Auto-application")
          val entity = random[GiftCard].copy(id = giftCard.id)

          urbanAirshipService.upsertTemplate[TemplateData.GiftCardTemplateData](any, any, any)(any, any) returns Future
            .successful(
              templateUpserted,
            )

          processor.execute(GiftCardChanged(merchant.id, entity))

          afterAWhile {
            there was one(urbanAirshipService).upsertTemplate(giftCard.id, PassType.Ios, templateData)

            val giftCardUpdated = giftCardDao.findById(giftCard.id).await.get
            giftCardUpdated.androidPayTemplateId ==== Some(templateUpserted.templateId)
            giftCardUpdated.appleWalletTemplateId ==== Some("iosFoo")
          }
        }
      }

    }

    "if record is not found" in new GiftCardChangedProcessorSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val entity = random[GiftCard]

      processor.execute(GiftCardChanged(merchant.id, entity))

      afterAWhile {
        there was noCallsTo(urbanAirshipService)
      }
    }
  }

  trait StateFixtures extends MultipleLocationFixtures {
    val giftCardProduct = Factory.giftCardProduct(merchant).create
    val templateUpserted = random[TemplateUpserted]
  }
}
