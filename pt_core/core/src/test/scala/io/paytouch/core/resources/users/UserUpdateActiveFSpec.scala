package io.paytouch.core.resources.users

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UserUpdateActiveFSpec extends UsersFSpec {

  "POST /v1/users.update_active" in {
    "if request has valid token" in {
      "if users belong to a location accessible by the current user" should {

        "disable/enable users globally" in new UserResourceFSpecContext {
          val userToDisableA = Factory.user(merchant, active = Some(true), locations = Seq(rome)).create
          val userToDisableB = Factory.user(merchant, active = Some(false), locations = Seq(london)).create
          val userToActivateA = Factory.user(merchant, active = Some(true), locations = Seq(rome)).create
          val userToActivateB = Factory.user(merchant, active = Some(false), locations = Seq(london)).create

          Factory.createValidTokenWithSession(userToDisableA)
          Factory.createValidTokenWithSession(userToDisableB)
          Factory.createValidTokenWithSession(userToActivateA)
          Factory.createValidTokenWithSession(userToActivateB)

          val userActiveUpdateItem = Seq(
            UpdateActiveItem(userToDisableA.id, false),
            UpdateActiveItem(userToDisableB.id, false),
            UpdateActiveItem(userToActivateA.id, true),
            UpdateActiveItem(userToActivateB.id, true),
          )

          Post(s"/v1/users.update_active", userActiveUpdateItem).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            afterAWhile {
              userDao.findById(userToDisableA.id).await.get.active should beFalse
              userDao.findById(userToDisableB.id).await.get.active should beFalse
              userDao.findById(userToActivateA.id).await.get.active should beTrue
              userDao.findById(userToActivateB.id).await.get.active should beTrue
            }

            sessionDao.findByUserId(userToDisableA.id).await ==== Seq.empty
            sessionDao.findByUserId(userToDisableB.id).await ==== Seq.empty
            sessionDao.findByUserId(userToActivateA.id).await.nonEmpty should beTrue
            sessionDao.findByUserId(userToActivateB.id).await.nonEmpty should beTrue
          }
        }

        "ignore request of disabling/enabling the logged user" in new UserResourceFSpecContext {
          val userActiveUpdateItem = Seq(UpdateActiveItem(user.id, false))

          Post(s"/v1/users.update_active", userActiveUpdateItem).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            userDao.findById(user.id).await.get.active should beTrue
          }
        }

        "ignore request of disabling/enabling a owner user" in new UserResourceFSpecContext {
          val owner =
            Factory.user(merchant, locations = Seq(rome, london), isOwner = Some(true), active = Some(true)).create
          val ownerActiveUpdateItem = Seq(UpdateActiveItem(owner.id, false))

          Post(s"/v1/users.update_active", ownerActiveUpdateItem).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            userDao.findById(owner.id).await.get.active should beTrue
          }
        }

        "if user doesn't belong to a location accessible by the current user" should {
          "not update the user and return 404" in new UserResourceFSpecContext {
            val newYork = Factory.location(merchant).create
            val userToDisable = Factory.user(merchant, active = Some(true), locations = Seq(newYork)).create
            val userActiveUpdateItem = Seq(
              UpdateActiveItem(userToDisable.id, false),
            )

            Post(s"/v1/users.update_active", userActiveUpdateItem).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)

              userDao.findById(userToDisable.id).await.get.active should beTrue
            }
          }
        }
      }
    }
  }
}
