package io.paytouch.core.async.monitors

import akka.actor.Actor
import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ ResettableUUID, UserContext, UserUpdate => UserUpdateEntity }
import io.paytouch.core.services.{ ImageUploadService, UserService }

final case class UserChange(
    state: UserService#State,
    update: UserUpdateEntity,
    userContext: UserContext,
  )

class UserMonitor(val imageUploadService: ImageUploadService) extends Actor {

  def receive: Receive = {
    case UserChange(state, update, _) => recordChange(state, update)
  }

  def recordChange(state: UserService#State, update: UserUpdateEntity) = {
    val (_, images) = state

    val userImage = images.find(_.objectType == ImageUploadType.User)
    recordUserImageChange(userImage, update.avatarImageId)
  }

  def recordUserImageChange(
      maybePreviousImageUpload: Option[ImageUploadRecord],
      maybeNewImageUploadId: ResettableUUID,
    ) =
    for {
      previousImageUpload <- maybePreviousImageUpload
      newImageUploadId <- maybeNewImageUploadId.value
      if !newImageUploadId.contains(previousImageUpload.id)
    } yield imageUploadService.deleteImage(previousImageUpload.id, ImageUploadType.User)

}
