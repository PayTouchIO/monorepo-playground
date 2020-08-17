package io.paytouch.core.resources.brands

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.resources.userroles.UserRolesFSpec

class UserRolesCreateFSpec extends UserRolesFSpec {

  abstract class UserRolesCreateFSpecContext extends UserRoleResourceFSpecContext

  "POST /v1/user_roles.create?user_role_id=$" in {
    "if request has valid token" in {

      "create brand and return 201" in new UserRolesCreateFSpecContext {
        val newUserRoleId = UUID.randomUUID
        val creation = random[UserRoleCreation]

        Post(s"/v1/user_roles.create?user_role_id=$newUserRoleId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val userRoleEntity = responseAs[ApiResponse[UserRole]].data
          val userRoleRecord = userRoleDao.findById(userRoleEntity.id).await.get
          assertResponse(userRoleEntity, userRoleRecord, withPermissions = true)
          assertCreation(newUserRoleId, creation)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new UserRolesCreateFSpecContext {
        val newUserRoleId = UUID.randomUUID
        val creation = random[UserRoleCreation]

        Post(s"/v1/user_roles.create?user_role_id=$newUserRoleId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
