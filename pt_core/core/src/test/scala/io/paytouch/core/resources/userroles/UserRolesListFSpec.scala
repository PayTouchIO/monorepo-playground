package io.paytouch.core.resources.userroles

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UserRolesListFSpec extends UserRolesFSpec {

  abstract class UserRolesListFSpecContext extends UserRoleResourceFSpecContext

  "GET /v1/user_roles.list" in {
    "if request has valid token" in {

      "with no parameters" should {
        "return all user roles without permissions" in new UserRolesListFSpecContext {
          val anotherUserRole = Factory.userRole(merchant).create

          Get(s"/v1/user_roles.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entities = responseAs[PaginatedApiResponse[Seq[UserRole]]].data
            entities.map(_.id) should containTheSameElementsAs(Seq(userRole.id, anotherUserRole.id))
            assertResponse(entities.find(_.id == userRole.id).get, userRole, withPermissions = false)
            assertResponse(entities.find(_.id == anotherUserRole.id).get, anotherUserRole, withPermissions = false)
          }
        }
      }

      "with expand[] users_count" should {
        "return all user roles without permissions and with users count" in new UserRolesListFSpecContext {
          val userRoleA = Factory.userRole(merchant).create

          val userRoleB = Factory.userRole(merchant).create
          Factory.user(merchant, userRole = Some(userRoleB)).create
          Factory.user(merchant, userRole = Some(userRoleB)).create

          Get(s"/v1/user_roles.list?expand[]=users_count").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entities = responseAs[PaginatedApiResponse[Seq[UserRole]]].data
            entities.map(_.id) should containTheSameElementsAs(Seq(userRole.id, userRoleA.id, userRoleB.id))
            assertResponse(
              entities.find(_.id == userRole.id).get,
              userRole,
              withPermissions = false,
              usersCount = Some(1),
            )
            assertResponse(
              entities.find(_.id == userRoleA.id).get,
              userRoleA,
              withPermissions = false,
              usersCount = Some(0),
            )
            assertResponse(
              entities.find(_.id == userRoleB.id).get,
              userRoleB,
              withPermissions = false,
              usersCount = Some(2),
            )
          }
        }
      }
    }
  }

}
