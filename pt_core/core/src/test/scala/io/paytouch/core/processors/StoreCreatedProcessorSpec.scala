package io.paytouch.core.processors

import com.softwaremill.macwire._
import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.messages.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class StoreCreatedProcessorSpec extends ProcessorSpec {

  abstract class StoreCreatedProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    val locationSettingsService = MockedRestApi.locationSettingsService
    val merchantService = MockedRestApi.merchantService
    val locationSettingsDao = daos.locationSettingsDao
    val merchantDao = daos.merchantDao

    val londonSettings = Factory
      .locationSettings(london, onlineStoreFrontEnabled = Some(false), deliveryProvidersEnabled = Some(false))
      .create
    val romeSettings = Factory
      .locationSettings(rome, onlineStoreFrontEnabled = Some(false), deliveryProvidersEnabled = Some(false))
      .create

    lazy val merchantId = merchant.id
    lazy val locationId = london.id

    lazy val processor = wire[StoreCreatedProcessor]

    def assertStepIsCompleted(step: MerchantSetupSteps) = {
      val reloadedMerchant = merchantDao.findById(merchant.id).await.get
      val setupSteps = reloadedMerchant.setupSteps
      val setupStep = setupSteps.flatMap(_.get(step)).get
      setupStep.completedAt must beSome
      setupStep.skippedAt must beNone
    }
  }

  "StoreCreatedProcessor" should {
    "type = storefront" should {
      "set onlineStorefrontEnabled location setting" in new StoreCreatedProcessorSpecContext {
        val msg = StoreCreated(
          merchantId.toString,
          StoreCreatedPayload(
            ExposedName.Store,
            merchantId,
            StoreCreatedData(locationId, `type` = StoreType.Storefront, provider = None),
          ),
        )
        processor.execute(msg)

        afterAWhile {
          val settings = locationSettingsDao.findByIds(Seq(londonSettings.id, romeSettings.id)).await
          settings.find(_.locationId == london.id).get.onlineStorefrontEnabled ==== true
          settings.find(_.locationId == london.id).get.deliveryProvidersEnabled ==== false
          settings.find(_.locationId == rome.id).get.onlineStorefrontEnabled ==== false
          settings.find(_.locationId == rome.id).get.deliveryProvidersEnabled ==== false
        }
      }

      "mark setupOnlineStore step as completed" in new StoreCreatedProcessorSpecContext {
        val msg = StoreCreated(
          merchantId.toString,
          StoreCreatedPayload(
            ExposedName.Store,
            merchantId,
            StoreCreatedData(locationId, `type` = StoreType.Storefront, provider = None),
          ),
        )
        processor.execute(msg)

        afterAWhile {
          assertStepIsCompleted(MerchantSetupSteps.SetupOnlineStore)
        }
      }
    }

    "type = delivery provider" should {
      "set deliveryProvidersEnabled location setting" in new StoreCreatedProcessorSpecContext {
        val msg = StoreCreated(
          merchantId.toString,
          StoreCreatedPayload(
            ExposedName.Store,
            merchantId,
            StoreCreatedData(
              locationId,
              `type` = StoreType.DeliveryProvider,
              provider = Some(DeliveryProvider.UberEats),
            ),
          ),
        )
        processor.execute(msg)

        afterAWhile {
          val settings = locationSettingsDao.findByIds(Seq(londonSettings.id, romeSettings.id)).await
          settings.find(_.locationId == london.id).get.onlineStorefrontEnabled ==== false
          settings.find(_.locationId == london.id).get.deliveryProvidersEnabled ==== true
          settings.find(_.locationId == rome.id).get.onlineStorefrontEnabled ==== false
          settings.find(_.locationId == rome.id).get.deliveryProvidersEnabled ==== false
        }
      }

      "mark connectDeliveryProvider step as completed" in new StoreCreatedProcessorSpecContext {
        val msg = StoreCreated(
          merchantId.toString,
          StoreCreatedPayload(
            ExposedName.Store,
            merchantId,
            StoreCreatedData(
              locationId,
              `type` = StoreType.DeliveryProvider,
              provider = Some(DeliveryProvider.UberEats),
            ),
          ),
        )
        processor.execute(msg)

        afterAWhile {
          assertStepIsCompleted(MerchantSetupSteps.ConnectDeliveryProvider)
        }
      }
    }
  }
}
