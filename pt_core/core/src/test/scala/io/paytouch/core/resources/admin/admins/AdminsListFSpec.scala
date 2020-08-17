package io.paytouch.core.resources.admin.admins

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class AdminsListFSpec extends AdminsFSpec {

  abstract class AdminsListFSpecContext extends AdminResourceFSpecContext

  "GET /v1/admin/admins.list" in {
    "if request has valid token" in {

      "return all the admin information without a password" in new AdminsListFSpecContext {

        Get("/v1/admin/admins.list?per_page=100").addHeader(adminAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val adminEntities = responseAs[PaginatedApiResponse[Seq[Admin]]].data
          val adminEntity = adminEntities.find(_.id == admin.id)
          adminEntity should beSome
          assertResponse(adminEntity.get, admin)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new AdminsListFSpecContext {

        Get(s"/v1/admin/admins.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
