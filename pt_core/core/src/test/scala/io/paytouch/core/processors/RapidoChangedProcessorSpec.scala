package io.paytouch.core.processors

import com.softwaremill.macwire._
import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.messages.entities.{ RapidoChanged, RapidoPayload }
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class RapidoChangedProcessorSpec extends ProcessorSpec {

  abstract class RapidoChangedProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    val locationSettingsService = MockedRestApi.locationSettingsService
    val locationSettingsDao = daos.locationSettingsDao

    val londonSettings = Factory.locationSettings(london, rapidoEnabled = Some(false)).create
    val romeSettings = Factory.locationSettings(rome, rapidoEnabled = Some(true)).create

    lazy val processor = wire[RapidoChangedProcessor]
  }

  "RapidoChangedProcessor" should {

    "update the location setting accordingly" in new RapidoChangedProcessorSpecContext {
      val merchantId = merchant.id
      val locationItems = Seq(UpdateActiveItem(london.id, active = true), UpdateActiveItem(rome.id, active = false))

      val payload = RapidoPayload(ExposedName.Store, merchantId, locationItems)
      val msg = RapidoChanged("rapido_changed", payload)
      processor.execute(msg)

      afterAWhile {
        val settings = locationSettingsDao.findByIds(Seq(londonSettings.id, romeSettings.id)).await
        settings.find(_.locationId == london.id).get.rapidoEnabled ==== true
        settings.find(_.locationId == rome.id).get.rapidoEnabled ==== false
      }
    }
  }

}
