package io.paytouch.core.resources.users

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class UsersGetFSpec extends UsersFSpec {

  abstract class UsersGetFSpecContext extends UserResourceFSpecContext {
    val otherUserRole = Factory
      .userRole(
        merchant,
        hasDashboardAccess = Some(genBoolean.instance),
        hasRegisterAccess = Some(genBoolean.instance),
        hasTicketsAccess = Some(genBoolean.instance),
      )
      .create
    val otherUser = Factory.user(merchant, userRole = Some(otherUserRole)).create
    Factory.userLocation(otherUser, rome).create
    Factory.userLocation(otherUser, london).create
  }

  "GET /v1/users.get?user_id=$" in {
    "if request has valid token" in {

      "if the user exists" should {

        "with no parameters" should {
          "return the user information without a password" in new UsersGetFSpecContext {

            Get(s"/v1/users.get?user_id=${otherUser.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val userEntity = responseAs[ApiResponse[User]].data
              assertResponse(userEntity, otherUser, withPermissions = false)
            }
          }

          "return user with image uploads" in new UsersGetFSpecContext {
            val imageUpload1 = Factory
              .imageUpload(merchant, Some(otherUser.id), Some(Map("a" -> "1", "b" -> "2")), Some(ImageUploadType.User))
              .create
            val imageUpload2 = Factory
              .imageUpload(merchant, Some(otherUser.id), Some(Map("c" -> "3", "d" -> "4")), Some(ImageUploadType.User))
              .create

            Get(s"/v1/users.get?user_id=${otherUser.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val userEntity = responseAs[ApiResponse[User]].data

              assertResponse(
                userEntity,
                otherUser,
                withPermissions = false,
                imageUploads = Seq(imageUpload1, imageUpload2),
              )
            }
          }

          "return 404 if user is deleted" in new UsersGetFSpecContext {
            val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now)).create

            Get(s"/v1/users.get?user_id=${deletedUser.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }

        "with expand[]=locations" should {
          "return the list of locations the user belongs to" in new UsersGetFSpecContext {
            val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
            Factory.userLocation(otherUser, deletedLocation).create

            Get(s"/v1/users.get?user_id=${otherUser.id}&expand[]=locations")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val userEntity = responseAs[ApiResponse[User]].data
              assertResponse(userEntity, otherUser, withPermissions = false)
              userEntity.locations.map(_.map(_.id)).get should containTheSameElementsAs(Seq(london.id, rome.id))
            }
          }
        }

        "with expand[]=merchant" should {
          "return the merchant the user belongs to" in new UsersGetFSpecContext {
            Get(s"/v1/users.get?user_id=${otherUser.id}&expand[]=merchant")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val userEntity = responseAs[ApiResponse[User]].data
              assertResponse(userEntity, otherUser, withPermissions = false, merchant = Some(merchant))
            }
          }
        }

        "with expand[]=access" should {
          "return the merchant the user belongs to" in new UsersGetFSpecContext {
            Get(s"/v1/users.get?user_id=${otherUser.id}&expand[]=access")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val userEntity = responseAs[ApiResponse[User]].data
              assertResponse(userEntity, otherUser, withPermissions = false, access = Some(buildAccess(otherUserRole)))
            }
          }
        }
      }

      "if the user does not belong to the merchant" should {
        "return 404" in new UsersGetFSpecContext {
          val competitor = Factory.merchant.create
          val userCompetitor = Factory.user(competitor).create

          Get(s"/v1/users.get?user_id=${userCompetitor.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user does not exist" should {
        "return 404" in new UsersGetFSpecContext {
          Get(s"/v1/users.get?user_id=${UUID.randomUUID}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
