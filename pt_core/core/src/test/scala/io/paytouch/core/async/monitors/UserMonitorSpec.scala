package io.paytouch.core.async.monitors

import akka.actor.Props
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.{ ResettableUUID, UserUpdate }
import io.paytouch.core.services.ImageUploadService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UserMonitorSpec extends MonitorSpec {

  abstract class UserMonitorSpecContext extends MonitorSpecContext with StateFixtures {

    val imageUploadService = mock[ImageUploadService]

    lazy val monitor = monitorSystem.actorOf(Props(new UserMonitor(imageUploadService)))
  }

  "UserMonitor" should {

    "delete old images that have been changed" in new UserMonitorSpecContext {
      val newUserImgUpload = Factory.imageUpload(merchant, Some(rome.id), None, Some(ImageUploadType.User)).create

      val update = random[UserUpdate].copy(avatarImageId = newUserImgUpload.id)

      monitor ! UserChange(state, update, userContext)

      afterAWhile {
        there was one(imageUploadService).deleteImage(userImageUpload.id, ImageUploadType.User)
      }
    }

    "delete old images that have been reset" in new UserMonitorSpecContext {
      val newUserImgUpload = Factory.imageUpload(merchant, Some(rome.id), None, Some(ImageUploadType.User)).create

      val update = random[UserUpdate].copy(avatarImageId = ResettableUUID.reset)

      monitor ! UserChange(state, update, userContext)

      afterAWhile {
        there was one(imageUploadService).deleteImage(userImageUpload.id, ImageUploadType.User)
      }
    }

    "do nothing if images have not changed" in new UserMonitorSpecContext {
      val update = random[UserUpdate].copy(avatarImageId = None)
      monitor ! UserChange(state, update, userContext)

      there was noCallsTo(imageUploadService)
    }
  }

  trait StateFixtures extends MonitorStateFixtures {
    val userImageUpload = Factory.imageUpload(merchant, Some(user.id), None, Some(ImageUploadType.User)).create
    val state = (user, Seq(userImageUpload))
  }
}
