package io.paytouch.core.resources.users

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UsersUpdateFSpec extends UsersFSpec {

  abstract class UsersUpdateFSpecContext extends UserResourceFSpecContext {
    val baseUpdate = random[UserUpdate].copy(
      email = None,
      pin = None,
      isOwner = None,
      avatarImageId = None,
      userRoleId = None,
      locationIds = None,
      locationOverrides = Map.empty,
    )

    protected def assertUserLocationIds(userId: UUID, locationIds: Seq[UUID]) = {
      val userLocations = userLocationDao.findByItemId(userId).await
      userLocations.map(_.locationId) should containTheSameElementsAs(locationIds)
    }
  }

  "POST /v1/users.update?user_id=$" in {
    "if request has valid token" in {
      "if user belong to same merchant" should {
        "update user and return 200" in new UsersUpdateFSpecContext {
          val avatarUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.User)).create

          val update = baseUpdate.copy(
            email = Some(randomEmail),
            pin = userPin,
            userRoleId = Some(userRole.id),
            locationIds = Some(Seq(rome.id)),
            avatarImageId = avatarUpload.id,
          )

          Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, user.id, user.isOwner, imageUpload = Some(avatarUpload))
          }
        }

        "update user with the same email and return 200" in new UsersUpdateFSpecContext {
          val avatarUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.User)).create

          val update = baseUpdate.copy(email = Some(user.email))

          Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, user.id, user.isOwner, imageUpload = None)
          }
        }

        "for a user owner, it shouldn't be possible to change the is_owner attribute" in new UsersUpdateFSpecContext {
          val owner = Factory.user(merchant, isOwner = Some(true), locations = Seq(london, rome)).create
          val avatarUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.User)).create

          val update = baseUpdate.copy(isOwner = Some(false))

          Post(s"/v1/users.update?user_id=${owner.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, owner.id, owner.isOwner, imageUpload = None)
          }
        }

        "for a user owner, it shouldn't be possible to remove user location associations" in new UsersUpdateFSpecContext {
          val owner = Factory.user(merchant, isOwner = Some(true), locations = Seq(london, rome)).create
          val avatarUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.User)).create

          val update = baseUpdate.copy(locationIds = Some(Seq(rome.id)))

          Post(s"/v1/users.update?user_id=${owner.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            assertUpdate(update, owner.id, owner.isOwner, imageUpload = None)
            val userLocations = userLocationDao.findByItemId(owner.id).await
            userLocations.map(_.locationId) should containTheSameElementsAs(Seq(rome.id, london.id))
          }
        }

        "update user by removing image" in new UsersUpdateFSpecContext {
          val avatarUpload = Factory
            .imageUpload(merchant, imageUploadType = Some(ImageUploadType.User), objectId = Some(user.id))
            .create

          val update = baseUpdate.copy(avatarImageId = ResettableUUID.reset)

          Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, user.id, user.isOwner, imageUpload = None)
          }
        }

        "update using location overrides" in new UsersUpdateFSpecContext {
          val newYork = Factory.location(merchant).create
          override lazy val user =
            Factory.user(merchant, isOwner = Some(false), locations = Seq(london, rome, newYork)).create

          val locationOverrides = Map(
            rome.id -> false,
            london.id -> true,
          )
          val update = baseUpdate.copy(locationOverrides = locationOverrides)

          Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, user.id, user.isOwner, imageUpload = None)
            assertUserLocationIds(user.id, Seq(london.id, newYork.id))
          }
        }

        "for a user owner, it shouldn't be possible to remove user location associations" in new UsersUpdateFSpecContext {
          val newYork = Factory.location(merchant).create
          override lazy val user =
            Factory.user(merchant, isOwner = Some(true), locations = Seq(london, rome, newYork)).create

          val locationOverrides = Map(
            rome.id -> false,
            london.id -> true,
          )
          val update = baseUpdate.copy(locationOverrides = locationOverrides)

          Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, user.id, user.isOwner, imageUpload = None)
            assertUserLocationIds(user.id, Seq(london.id, rome.id, newYork.id))
          }
        }

        "pin is already taken" should {
          "return 400" in new UsersUpdateFSpecContext {
            val pin = "2222"
            val existingUser = Factory.user(merchant, pin = Some(pin)).create

            val update = baseUpdate.copy(pin = pin)

            Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCodesAtLeastOnce("PinAlreadyInUse")
            }
          }
        }

        "password is too short" should {
          "return 400" in new UsersUpdateFSpecContext {
            val existingUser = Factory.user(merchant).create

            val update = baseUpdate.copy(password = Some("1234567"))

            Post(s"/v1/users.update?user_id=${user.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCodesAtLeastOnce("InvalidPassword")
            }
          }
        }
      }

      "if user doesn't belong to current user's merchant" in {
        "not update user and return 404" in new UsersUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create

          val update = random[UserUpdate]

          Post(s"/v1/users.update?user_id=${competitorUser.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedUser = userDao.findById(competitorUser.id).await.get
            updatedUser ==== competitorUser
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new UsersUpdateFSpecContext {
        val userId = UUID.randomUUID
        val update = random[UserUpdate]
        Post(s"/v1/users.update?user_id=$userId", update).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
