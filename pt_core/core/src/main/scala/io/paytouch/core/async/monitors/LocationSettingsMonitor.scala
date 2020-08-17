package io.paytouch.core.async.monitors

import java.util.UUID

import scala.concurrent._

import akka.actor.Actor

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.entities._
import io.paytouch.core.services._

final case class LocationSettingsChange(
    state: LocationSettingsService#State,
    update: LocationSettingsUpdate,
    userContext: UserContext,
  )

class LocationSettingsMonitor(imageUploadService: ImageUploadService) extends Actor {
  def receive: Receive = {
    case LocationSettingsChange((locationSettingsRecord, images), update, userContext) =>
      recordChange(images, update)
  }

  private def recordChange(images: Seq[ImageUploadRecord], update: LocationSettingsUpdate): Unit = {
    recordEmailReceiptChange(
      maybeImageUpload = images.find(_.objectType == ImageUploadType.EmailReceipt),
      maybeUpdate = update.locationEmailReceipt,
    )

    recordPrintReceiptChange(
      maybeImageUpload = images.find(_.objectType == ImageUploadType.PrintReceipt),
      maybeUpdate = update.locationPrintReceipt,
    )
  }

  private def recordEmailReceiptChange(
      maybeImageUpload: Option[ImageUploadRecord],
      maybeUpdate: Option[LocationEmailReceiptUpdate],
    ): Option[Future[Seq[UUID]]] =
    for {
      previousImageUpload <- maybeImageUpload
      update <- maybeUpdate
      newImageUploadId <- update.imageUploadId.toOption
      if newImageUploadId != previousImageUpload.id
    } yield imageUploadService.deleteImage(previousImageUpload.id, ImageUploadType.EmailReceipt)

  def recordPrintReceiptChange(
      maybeImageUpload: Option[ImageUploadRecord],
      maybeUpdate: Option[LocationPrintReceiptUpdate],
    ): Option[Future[Seq[UUID]]] =
    for {
      previousImageUpload <- maybeImageUpload
      update <- maybeUpdate
      newImageUploadId <- update.imageUploadId.toOption
      if newImageUploadId != previousImageUpload.id
    } yield imageUploadService.deleteImage(previousImageUpload.id, ImageUploadType.PrintReceipt)
}
