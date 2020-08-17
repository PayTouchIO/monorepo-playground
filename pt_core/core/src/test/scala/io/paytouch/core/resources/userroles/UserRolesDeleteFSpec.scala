package io.paytouch.core.resources.userroles

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UserRolesDeleteFSpec extends UserRolesFSpec {

  lazy val userDao = daos.userDao

  "POST /v1/user_roles.delete" in {
    "if request has valid token" in {
      "delete user roles and nullify its relation with users" in new UserRoleResourceFSpecContext {
        val role = Factory.userRole(merchant).create
        val userWithRole = Factory.user(merchant, userRole = Some(role)).create

        Post(s"/v1/user_roles.delete", Ids(ids = Seq(role.id))).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          userRoleDao.findById(role.id).await should beEmpty
          userDao.findById(userWithRole.id).await.get.userRoleId should beNone
        }
      }
    }
  }
}
