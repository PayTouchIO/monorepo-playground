package io.paytouch.core.services

import com.softwaremill.macwire._

import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.core.async.monitors.AuthenticationMonitor
import io.paytouch.core.async.sqs._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.entities.enums.MerchantSetupSteps._
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.messages.entities.{ MerchantChanged, MerchantPayload }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }
import io.paytouch.utils.Tagging._

@scala.annotation.nowarn("msg=Auto-application")
class MerchantServiceSpec extends ServiceDaoSpec {
  abstract class MerchantServiceSpecContext extends ServiceDaoSpecContext {
    implicit val logger = new PaytouchLogger

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val bcryptRounds = Config.bcryptRounds

    // We don't care about sample data being created for demo merchants
    override val sampleDataService = mock[SampleDataService]

    val authenticationMonitor = actorMock.ref.taggedWith[AuthenticationMonitor]
    val authenticationService: AuthenticationService = wire[AuthenticationService]

    lazy val adminService: AdminMerchantService = wire[AdminMerchantService]
    val service = wire[MerchantService]

    val merchantDao = daos.merchantDao

    val allCompletedOrSkippedSetupStepsRestaurant: Map[MerchantSetupSteps, MerchantSetupStep] = MerchantSetupSteps
      .forBusinessType(BusinessType.Restaurant)
      .map(key => key -> MerchantSetupStep(skippedAt = Some(UtcTime.now)))
      .toMap

  }

  "MerchantService" in {
    "enrich" should {
      "provide an empty map with all steps in setupSteps" in new MerchantServiceSpecContext {
        val expectedLocationName = "foobar"
        val expectedLogoUrls = Map("a" -> "b")

        val locationReceipt =
          Factory
            .locationReceipt(rome, locationName = Some(expectedLocationName))
            .create

        val imageUpload =
          Factory
            .imageUpload(
              merchant,
              objectId = Some(rome.id),
              imageUploadType = Some(ImageUploadType.EmailReceipt),
              urls = Some(expectedLogoUrls),
            )
            .create

        val merchantEntity =
          service
            .enrich(merchant, service.defaultFilters)(MerchantExpansions.none.copy(withSetupSteps = true))
            .await

        val allSteps =
          MerchantSetupSteps
            .values
            .map(_ -> MerchantSetupStatus.Pending)
            .toMap

        merchantEntity.logoUrls ==== Seq(ImageUrls(imageUpload.id, expectedLogoUrls))
        merchantEntity.setupSteps ==== Some(allSteps)
      }

      "fromRecordAndOptionToEntity" should {
        "merge features depending on SetupType and record level features" should {
          "pos = false + setupType = Paytouch" in new MerchantServiceSpecContext {
            val merchantFeatures = random[MerchantFeatures].copy(pos = MerchantFeature(false))
            val merchantRecord =
              random[MerchantRecord].copy(setupType = SetupType.Paytouch, features = merchantFeatures)
            service.fromRecordAndOptionToEntity(merchantRecord).features.pos.enabled must beTrue
          }
          "pos = false + setupType = Dash" in new MerchantServiceSpecContext {
            val merchantFeatures = random[MerchantFeatures].copy(pos = MerchantFeature(false))
            val merchantRecord = random[MerchantRecord].copy(setupType = SetupType.Dash, features = merchantFeatures)
            service.fromRecordAndOptionToEntity(merchantRecord).features.pos.enabled must beFalse
          }
          "pos = true + setupType = Paytouch" in new MerchantServiceSpecContext {
            val merchantFeatures = random[MerchantFeatures].copy(pos = MerchantFeature(true))
            val merchantRecord =
              random[MerchantRecord].copy(setupType = SetupType.Paytouch, features = merchantFeatures)
            service.fromRecordAndOptionToEntity(merchantRecord).features.pos.enabled must beTrue
          }
          "pos = true + setupType = Dash" in new MerchantServiceSpecContext {
            val merchantFeatures = random[MerchantFeatures].copy(pos = MerchantFeature(true))
            val merchantRecord = random[MerchantRecord].copy(setupType = SetupType.Dash, features = merchantFeatures)
            service.fromRecordAndOptionToEntity(merchantRecord).features.pos.enabled must beTrue
          }
        }
      }
    }

    "resetSetupStep" should {
      "trigger setupCompleted = false if all steps were skipped or completed and one gets reset (restaurant)" in new MerchantServiceSpecContext {
        override lazy val merchant = Factory.merchant(businessType = Some(BusinessType.Restaurant)).create

        merchantDao
          .updateSetupSteps(
            merchant.id,
            setupCompleted = true,
            updatedSteps = allCompletedOrSkippedSetupStepsRestaurant,
          )
          .await

        service.resetSetupStep(DesignReceipts).await
        val reloadedMerchant1 = merchantDao.findById(merchant.id).await.get
        reloadedMerchant1.setupCompleted ==== false
      }
    }

    "skipSetupStep" should {
      "trigger setupCompleted = true if all steps are skipped or completed (restaurant)" in new MerchantServiceSpecContext {
        override lazy val merchant = Factory.merchant(businessType = Some(BusinessType.Restaurant)).create

        val currentSteps: Map[MerchantSetupSteps, MerchantSetupStep] =
          allCompletedOrSkippedSetupStepsRestaurant.filterNot(p => Seq(DesignReceipts, SetupKitchens).contains(p._1))
        merchantDao.updateSetupSteps(merchant.id, setupCompleted = false, updatedSteps = currentSteps).await

        service.skipSetupStep(DesignReceipts).await
        val reloadedMerchant1 = merchantDao.findById(merchant.id).await.get
        reloadedMerchant1.setupCompleted ==== false

        service.skipSetupStep(SetupKitchens).await
        val reloadedMerchant2 = merchantDao.findById(merchant.id).await.get
        reloadedMerchant2.setupCompleted ==== true
      }

      "trigger setupCompleted = true if all steps are skipped or completed (retail)" in new MerchantServiceSpecContext {
        override lazy val merchant = Factory.merchant(businessType = Some(BusinessType.Retail)).create

        val setupSteps =
          Map[MerchantSetupSteps, MerchantSetupStep](
            SetupLocations -> MerchantSetupStep(skippedAt = Some(UtcTime.now)),
            ImportProducts -> MerchantSetupStep(completedAt = Some(UtcTime.now)),
            ImportCustomers -> MerchantSetupStep(skippedAt = Some(UtcTime.now)),
            SetupEmployees -> MerchantSetupStep(completedAt = Some(UtcTime.now)),
            ScheduleEmployees -> MerchantSetupStep(skippedAt = Some(UtcTime.now)),
          )
        merchantDao.updateSetupSteps(merchant.id, setupCompleted = false, updatedSteps = setupSteps).await

        service.skipSetupStep(DesignReceipts).await
        val reloadedMerchant = merchantDao.findById(merchant.id).await.get
        reloadedMerchant.setupCompleted ==== true
      }

      "trigger setupCompleted = true if merchant is in demo mode" in new MerchantServiceSpecContext {
        override lazy val merchant = Factory.merchant(mode = Some(MerchantMode.Demo)).create

        val setupSteps = Map.empty[MerchantSetupSteps, MerchantSetupStep]
        merchantDao.updateSetupSteps(merchant.id, setupCompleted = false, updatedSteps = setupSteps).await

        service.skipSetupStep(DesignReceipts).await

        val reloadedMerchant = merchantDao.findById(merchant.id).await.get
        reloadedMerchant.setupCompleted ==== true
      }
    }

    "update" should {
      "cascade business name change to gift cards and loyalty programs" in new MerchantServiceSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct).create
        val loyaltyProgram = Factory.loyaltyProgram(merchant).create

        val updatedBusinessName = "new business name"

        val update = ApiMerchantUpdate(
          businessName = Some(updatedBusinessName),
          restaurantType = None,
          zoneId = None,
          legalDetails = None,
        )

        service.update(merchant.id, update).await

        afterAWhile {
          daos.giftCardDao.findById(giftCard.id).await.get.businessName ==== updatedBusinessName
          daos.loyaltyProgramDao.findById(loyaltyProgram.id).await.get.businessName ==== updatedBusinessName
        }
      }
    }

    "setWorldpayConfig" should {
      "sendMerchantChangedMessage" in new MerchantServiceSpecContext {
        service.setWorldpayConfig(random[WorldpayConfigUpsertion]).await.success

        val entity = service.findById(merchant.id)(merchantService.defaultExpansions).await.get
        val reloadedMerchantRecord = merchantDao.findById(merchant.id).await.get
        actorMock.expectMsg(SendMsgWithRetry(MerchantChanged(entity, reloadedMerchantRecord.paymentProcessorConfig)))
      }

      "not sendMerchantChangedMessage for demo merchants" in new MerchantServiceSpecContext {
        override lazy val merchant = Factory.merchant(mode = Some(MerchantMode.Demo)).create

        service.setWorldpayConfig(random[WorldpayConfigUpsertion]).await.success

        actorMock.expectNoMessage()
      }
    }

    "resetPaymentProcessor" should {
      "sendMerchantChangedMessage" in new MerchantServiceSpecContext {
        service.resetPaymentProcessor().await.success

        val entity = service.findById(merchant.id)(merchantService.defaultExpansions).await.get
        val reloadedMerchantRecord = merchantDao.findById(merchant.id).await.get
        actorMock.expectMsg(SendMsgWithRetry(MerchantChanged(entity, reloadedMerchantRecord.paymentProcessorConfig)))
      }
    }

    "switchModeTo" should {
      "mode = production" should {
        "send a message to ordering for the new merchant" in new MerchantServiceSpecContext {
          override lazy val merchant = Factory.merchant(mode = Some(MerchantMode.Demo)).create

          service.switchModeTo(MerchantMode.Production).await.success

          val oldMerchant = merchantDao.findById(merchant.id).await.get
          oldMerchant.mode ==== MerchantMode.Demo
          oldMerchant.switchMerchantId must beSome

          val newMerchant = merchantDao.findById(oldMerchant.switchMerchantId.get).await.get
          newMerchant.mode ==== MerchantMode.Production
          newMerchant.switchMerchantId === Some(oldMerchant.id)

          // We need to use expectMsgPf as timestamps of the message entity are
          // different from the record for some reason
          actorMock.expectMsgPF() {
            case SendMsgWithRetry(MerchantChanged(_, MerchantPayload(_, _, entity, _)))
                if entity.id ==== newMerchant.id =>
              true
          }
        }
      }
    }
  }
}
