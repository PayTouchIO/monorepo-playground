package io.paytouch.core.resources.userroles

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UserRolesUpdateFSpec extends UserRolesFSpec {

  abstract class UserRolesUpdateFSpecContext extends UserRoleResourceFSpecContext

  "POST /v1/user_roles.update?user_role_id=$" in {
    "if request has valid token" in {
      "if user role belong to same merchant" should {
        "update user role and return 200" in new UserRolesUpdateFSpecContext {
          val myUserRole = Factory.userRole(merchant).create
          val update = random[UserRoleUpdate]

          Post(s"/v1/user_roles.update?user_role_id=${myUserRole.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[UserRole]].data
            val record = userRoleDao.findById(entity.id).await.get
            assertResponse(entity, record, withPermissions = true)
            assertUpdate(entity.id, update)
          }
        }
      }
      "if user role doesn't belong to current user's merchant" in {
        "not update user role and return 404" in new UserRolesUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUserRole = Factory.userRole(competitor).create

          val update = random[UserRoleUpdate]

          Post(s"/v1/user_roles.update?user_role_id=${competitorUserRole.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            val updatedUserRole = userRoleDao.findById(competitorUserRole.id).await.get
            updatedUserRole ==== competitorUserRole
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new UserRolesUpdateFSpecContext {
        val userRoleId = UUID.randomUUID
        val update = random[UserRoleUpdate]
        Post(s"/v1/user_roles.update?user_role_id=$userRoleId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
