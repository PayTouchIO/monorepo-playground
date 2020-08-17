package io.paytouch.core.resources.users

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import org.scalacheck.Gen

class UsersCreateFSpec extends UsersFSpec {

  abstract class UsersCreateFSpecContext extends UserResourceFSpecContext

  "POST /v1/users.create?user_id=$" in {
    "if request has valid token" in {
      "email is new" should {
        "create user and return 201" in new UsersCreateFSpecContext {
          val newUserId = UUID.randomUUID
          val avatarUpload = Factory.imageUpload(merchant, imageUploadType = Some(ImageUploadType.User)).create
          val creation = random[UserCreation].copy(
            email = randomEmail.toUpperCase,
            pin = Gen.option(genNumericString(4)).instance,
            userRoleId = Some(userRole.id),
            locationIds = Some(Seq(rome.id)),
            avatarImageId = avatarUpload.id,
          )

          Post(s"/v1/users.create?user_id=$newUserId", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val user = responseAs[ApiResponse[User]].data
            user.id ==== newUserId
            assertCreation(creation, user.id, imageUpload = Some(avatarUpload))
          }
        }

        "if user owner, create user and associate user to all locations" in new UsersCreateFSpecContext {
          val newUserId = UUID.randomUUID
          val creation = random[UserCreation].copy(
            email = randomEmail,
            pin = Gen.option(genNumericString(4)).instance,
            userRoleId = Some(userRole.id),
            locationIds = None,
            avatarImageId = None,
            isOwner = Some(true),
          )

          Post(s"/v1/users.create?user_id=$newUserId", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val user = responseAs[ApiResponse[User]].data
            user.id ==== newUserId
            assertCreation(creation, user.id, imageUpload = None)

            val userLocations = userLocationDao.findByItemId(newUserId).await
            userLocations.map(_.locationId) should containTheSameElementsAs(Seq(rome.id, london.id))
          }
        }
      }

      "email is invalid" should {
        "return 400" in new UsersCreateFSpecContext {
          val newUserId = UUID.randomUUID
          val creation = random[UserCreation].copy(
            email = "yadda",
            pin = Gen.option(genNumericString(4)).instance,
            userRoleId = Some(userRole.id),
            locationIds = Some(Seq(rome.id)),
            avatarImageId = None,
          )

          Post(s"/v1/users.create?user_id=$newUserId", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }

      "pin is already taken" should {
        "return 400" in new UsersCreateFSpecContext {
          val pin = "2222"
          val existingUser = Factory.user(merchant, pin = Some(pin)).create

          val newUserId = UUID.randomUUID
          val creation = random[UserCreation].copy(
            email = randomEmail,
            pin = Some(pin),
            userRoleId = Some(userRole.id),
            locationIds = Some(Seq(rome.id)),
            avatarImageId = None,
          )

          Post(s"/v1/users.create?user_id=$newUserId", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("PinAlreadyInUse")
          }
        }
      }

      "email already exists" should {
        "return 400" in new UsersCreateFSpecContext {
          val existingUser = Factory.user(merchant, email = Some("test@paytouch.io")).create

          val newUserId = UUID.randomUUID
          val creation = random[UserCreation].copy(
            email = existingUser.email,
            pin = Gen.option(genNumericString(4)).instance,
            userRoleId = Some(userRole.id),
            locationIds = Some(Seq(rome.id)),
            avatarImageId = None,
          )

          Post(s"/v1/users.create?user_id=$newUserId", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("EmailAlreadyInUse")
          }
        }
      }

      "password shorter than 8 characters" should {
        "return 400" in new UsersCreateFSpecContext {
          val newUserId = UUID.randomUUID
          val creation = random[UserCreation].copy(
            password = "1234567",
            email = randomEmail,
            userRoleId = Some(userRole.id),
            locationIds = Some(Seq(rome.id)),
          )

          Post(s"/v1/users.create?user_id=$newUserId", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidPassword")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new UsersCreateFSpecContext {
        val newUserId = UUID.randomUUID
        val creation = random[UserCreation]
        Post(s"/v1/users.create?user_id=$newUserId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
