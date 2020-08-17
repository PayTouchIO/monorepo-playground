package io.paytouch.core.resources.admin.admins

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class AdminsCreateFSpec extends AdminsFSpec {
  abstract class AdminsCreateFSpecContext extends AdminResourceFSpecContext

  "POST /v1/admin/admins.create?admin_id=$" in {
    "if request has valid token" in {
      "email is new" should {
        "create admin and return 201" in new AdminsCreateFSpecContext {
          val newAdminId = UUID.randomUUID
          val creation = random[AdminCreation].copy(email = randomEmail)

          Post(s"/v1/admin/admins.create?admin_id=$newAdminId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val admin = responseAs[ApiResponse[Admin]].data
            admin.id ==== newAdminId
            assertCreation(creation, admin.id)
          }
        }
      }

      "email already exists" should {
        "return 400" in new AdminsCreateFSpecContext {
          val existingAdmin = Factory.admin(email = Some("test@paytouch.io")).create

          val newAdminId = UUID.randomUUID
          val creation = random[AdminCreation].copy(email = existingAdmin.email)

          Post(s"/v1/admin/admins.create?admin_id=$newAdminId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("EmailAlreadyInUse")
          }
        }
      }

      "invalid email" should {
        "return 400" in new AdminsCreateFSpecContext {
          val newAdminId = UUID.randomUUID
          val creation = random[AdminCreation].copy(email = "yadda")

          Post(s"/v1/admin/admins.create?admin_id=$newAdminId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }

      "short password" should {
        "return 400" in new AdminsCreateFSpecContext {
          val newAdminId = UUID.randomUUID
          val creation = random[AdminCreation].copy(password = "1234567")

          Post(s"/v1/admin/admins.create?admin_id=$newAdminId", creation)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidPassword")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new AdminsCreateFSpecContext {
        val newAdminId = UUID.randomUUID
        val creation = random[AdminCreation]
        Post(s"/v1/admin/admins.create?admin_id=$newAdminId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
