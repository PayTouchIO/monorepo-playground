package io.paytouch.core.processors

import com.softwaremill.macwire._
import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.entities.enums._
import io.paytouch.core.messages.entities.{ StoresActiveChanged, StoresActivePayload }
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class StoresActiveChangedProcessorSpec extends ProcessorSpec {

  abstract class StoresActiveChangedProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    val locationSettingsService = MockedRestApi.locationSettingsService
    val merchantService = MockedRestApi.merchantService
    val locationSettingsDao = daos.locationSettingsDao
    val merchantDao = daos.merchantDao

    val londonSettings = Factory.locationSettings(london, onlineStoreFrontEnabled = Some(false)).create
    val romeSettings = Factory.locationSettings(rome, onlineStoreFrontEnabled = Some(false)).create

    lazy val processor = wire[StoresActiveChangedProcessor]

    def assertStepIsCompleted(step: MerchantSetupSteps) = {
      val reloadedMerchant = merchantDao.findById(merchant.id).await.get
      val setupSteps = reloadedMerchant.setupSteps
      val setupStep = setupSteps.flatMap(_.get(step)).get
      setupStep.completedAt must beSome
      setupStep.skippedAt must beNone
    }
  }

  "StoresActiveChangedProcessor" should {
    "set onlineStorefrontEnabled location setting" in new StoresActiveChangedProcessorSpecContext {
      val merchantId = merchant.id
      val locationItems = Seq(UpdateActiveItem(london.id, active = true), UpdateActiveItem(rome.id, active = false))

      val payload = StoresActivePayload(ExposedName.Store, merchantId, locationItems)
      val msg = StoresActiveChanged("stores_active_changed", payload)
      processor.execute(msg)

      afterAWhile {
        val settings = locationSettingsDao.findByIds(Seq(londonSettings.id, romeSettings.id)).await
        settings.find(_.locationId == london.id).get.onlineStorefrontEnabled ==== true
        settings.find(_.locationId == rome.id).get.onlineStorefrontEnabled ==== false
      }
    }

    "mark setupOnlineStore step as completed" in new StoresActiveChangedProcessorSpecContext {
      val merchantId = merchant.id
      val locationItems = Seq(UpdateActiveItem(london.id, active = true), UpdateActiveItem(rome.id, active = false))

      val payload = StoresActivePayload(ExposedName.Store, merchantId, locationItems)
      val msg = StoresActiveChanged("stores_active_changed", payload)
      processor.execute(msg)

      afterAWhile {
        assertStepIsCompleted(MerchantSetupSteps.SetupOnlineStore)
      }
    }
  }

}
