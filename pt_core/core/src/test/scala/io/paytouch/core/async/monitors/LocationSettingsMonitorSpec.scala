package io.paytouch.core.async.monitors

import akka.actor.Props
import akka.testkit.TestProbe

import io.paytouch.core.async.sqs._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities.LocationSettingsUpdated
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.ImageUploadService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class LocationSettingsMonitorSpec extends MonitorSpec {
  abstract class LocationSettingsMonitorSpecContext extends MonitorSpecContext with StateFixtures {
    val imageUploadService = mock[ImageUploadService]

    val actorMock =
      new TestProbe(monitorSystem)

    val messageHandler =
      new SQSMessageHandler(
        monitorSystem,
        actorMock.ref.taggedWith[SQSMessageSender],
      )

    lazy val monitor =
      monitorSystem
        .actorOf(Props(new LocationSettingsMonitor(imageUploadService)))
  }

  "LocationSettingsMonitor" should {
    "delete old images" in new LocationSettingsMonitorSpecContext {
      val newEmailImgUpload =
        Factory
          .imageUpload(merchant, Some(rome.id), None, Some(ImageUploadType.EmailReceipt))
          .create

      val newPrintImgUpload =
        Factory
          .imageUpload(merchant, Some(rome.id), None, Some(ImageUploadType.PrintReceipt))
          .create

      val locationEmailReceiptUpdate =
        random[LocationEmailReceiptUpdate].copy(imageUploadId = newEmailImgUpload.id)

      val locationPrintReceiptUpdate =
        random[LocationPrintReceiptUpdate].copy(imageUploadId = newPrintImgUpload.id)

      @scala.annotation.nowarn("msg=Auto-application")
      val update = random[LocationSettingsUpdate].copy(
        locationEmailReceipt = Some(locationEmailReceiptUpdate),
        locationPrintReceipt = Some(locationPrintReceiptUpdate),
      )

      monitor ! LocationSettingsChange(state, update, userContext)

      afterAWhile {
        there was one(imageUploadService).deleteImage(emailReceiptImageUpload.id, ImageUploadType.EmailReceipt)
        there was one(imageUploadService).deleteImage(printReceiptImageUpload.id, ImageUploadType.PrintReceipt)

      }
    }

    "do nothing if images have not changed" in new LocationSettingsMonitorSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val update = random[LocationSettingsUpdate].copy(locationEmailReceipt = None, locationPrintReceipt = None)
      monitor ! LocationSettingsChange(state, update, userContext)

      there were noCallsTo(imageUploadService)
      actorMock.expectNoMessage()
    }
  }

  trait StateFixtures extends MonitorStateFixtures {
    val romeSettings = Factory.locationSettings(rome).create

    val emailReceiptImageUpload =
      Factory
        .imageUpload(merchant, Some(rome.id), None, Some(ImageUploadType.EmailReceipt))
        .create

    val printReceiptImageUpload =
      Factory
        .imageUpload(merchant, Some(rome.id), None, Some(ImageUploadType.PrintReceipt))
        .create

    val state = (romeSettings, Seq(emailReceiptImageUpload, printReceiptImageUpload))
  }
}
