package io.paytouch.core.services

import com.softwaremill.macwire._

import io.paytouch.core.async.monitors._
import io.paytouch.core.async.sqs.SendMsgWithRetry
import io.paytouch.core.async.sqs.SQSMessageSender
import io.paytouch.core.entities.LocationSettingsUpdate
import io.paytouch.core.messages.entities.LocationSettingsUpdated
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class LocationSettingsServiceSpec extends ServiceDaoSpec {
  abstract class LocationSettingsServiceSpecContext extends ServiceDaoSpecContext {
    val locationSettingsMonitor = actorMock.ref.taggedWith[LocationSettingsMonitor]

    val messageHandler =
      new SQSMessageHandler(
        actorSystem,
        actorMock.ref.taggedWith[SQSMessageSender],
      )
    val service = wire[LocationSettingsService]
    val locationSettings = Factory.locationSettings(rome).create
  }

  "LocationSettingsService" in {
    "update" should {
      "if successful" should {
        "notify a location settings change" in new LocationSettingsServiceSpecContext {
          @scala.annotation.nowarn("msg=Auto-application")
          val update = random[LocationSettingsUpdate]

          val (_, locationSettingsEntity) = service.update(rome.id, update).await.success

          val state = (locationSettings, Seq.empty)
          actorMock.expectMsg(LocationSettingsChange(state, update, userContext))

          actorMock.expectMsg(
            SendMsgWithRetry(LocationSettingsUpdated(state._1.locationId)(userContext)),
          )
        }
      }
    }
  }
}
