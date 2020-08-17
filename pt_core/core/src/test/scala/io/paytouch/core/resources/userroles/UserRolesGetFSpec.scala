package io.paytouch.core.resources.userroles

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UserRolesGetFSpec extends UserRolesFSpec {

  abstract class UserRolesGetFSpecContext extends UserRoleResourceFSpecContext

  "GET /v1/user_roles.get?user_role_id=$" in {
    "if request has valid token" in {

      "if the user role exists" should {

        "with no parameters" should {
          "return the user roles with permissions" in new UserRolesGetFSpecContext {
            val userRoleRecord = Factory.userRole(merchant).create

            Get(s"/v1/user_roles.get?user_role_id=${userRoleRecord.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val userRoleEntity = responseAs[ApiResponse[UserRole]].data
              assertResponse(userRoleEntity, userRoleRecord, withPermissions = true)
            }
          }
        }

        "with expand[] users_count" should {
          "return the user roles with permissions and users_count" in new UserRolesGetFSpecContext {
            val userRoleRecord = Factory.userRole(merchant).create
            Factory.user(merchant, userRole = Some(userRoleRecord)).create
            Factory.user(merchant, userRole = Some(userRoleRecord)).create
            Factory.user(merchant, userRole = Some(userRoleRecord)).create

            Get(s"/v1/user_roles.get?user_role_id=${userRoleRecord.id}&expand[]=users_count")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val userRoleEntity = responseAs[ApiResponse[UserRole]].data
              assertResponse(userRoleEntity, userRoleRecord, withPermissions = true, usersCount = Some(3))
            }
          }
        }
      }

      "if the user role does not belong to the merchant" should {
        "return 404" in new UserRolesGetFSpecContext {
          val competitor = Factory.merchant.create
          val userRoleCompetitor = Factory.userRole(competitor).create

          Get(s"/v1/user_roles.get?user_role_id=${userRoleCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user role does not exist" should {
        "return 404" in new UserRolesGetFSpecContext {
          Get(s"/v1/user_roles.get?user_role_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
