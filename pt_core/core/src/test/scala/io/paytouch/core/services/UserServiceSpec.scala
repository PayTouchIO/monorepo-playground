package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.monitors.{ UserChange, UserMonitor }
import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.entities.UserUpdate
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.utils.Tagging._

class UserServiceSpec extends ServiceDaoSpec {
  abstract class UserServiceSpecContext extends ServiceDaoSpecContext {
    val bcryptRounds = Config.bcryptRounds

    val userMonitor = actorMock.ref.taggedWith[UserMonitor]
    val service = wire[UserService]
    val userDao = daos.userDao

    def assertUserEmailWasRenamed(user: UserRecord, email: String) =
      userDao.findById(user.id).await.get.email ==== email
  }

  "UserService" in {
    "update" should {
      "if successful" should {
        "send the correct message" in new UserServiceSpecContext {
          val update =
            random[UserUpdate].copy(
              avatarImageId = None,
              email = Some(randomEmail),
              pin = randomNumericString,
              userRoleId = None,
            )

          val (_, userEntity) = service.update(user.id, update).await.success

          val state = (user, Seq.empty)
          actorMock.expectMsg(UserChange(state, update, userContext))
        }
      }

      "if validation fails" should {
        "not send any message" in new UserServiceSpecContext {
          val update = random[UserUpdate].copy(avatarImageId = UUID.randomUUID)

          service.update(user.id, update).await.failures

          actorMock.expectNoMessage()
        }
      }
    }

    "renameEmailsByMerchantId" should {
      "should add a prefix to all emails" in new UserServiceSpecContext {
        val user2 = Factory.user(merchant).create
        val user3 = Factory.user(merchant).create

        service.renameEmailsByMerchantId(merchant.id, "FOO-").await

        assertUserEmailWasRenamed(user, s"FOO-${user.email}")
        assertUserEmailWasRenamed(user2, s"FOO-${user2.email}")
        assertUserEmailWasRenamed(user3, s"FOO-${user3.email}")
      }
    }
  }
}
