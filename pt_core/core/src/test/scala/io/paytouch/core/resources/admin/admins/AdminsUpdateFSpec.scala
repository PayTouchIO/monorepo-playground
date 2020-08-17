package io.paytouch.core.resources.admin.admins

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class AdminsUpdateFSpec extends AdminsFSpec {

  abstract class AdminsUpdateFSpecContext extends AdminResourceFSpecContext

  "POST /v1/admin/admins.update?admin_id=$" in {
    "if request has valid token" in {
      "if admin belong to same merchant" should {
        "update admin and return 200" in new AdminsUpdateFSpecContext {
          val update = random[AdminUpdate].copy(email = Some(randomEmail))

          Post(s"/v1/admin/admins.update?admin_id=${admin.id}", update)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, admin.id)
          }
        }

        "update admin with the same email and return 200" in new AdminsUpdateFSpecContext {
          val update = random[AdminUpdate].copy(email = Some(admin.email))

          Post(s"/v1/admin/admins.update?admin_id=${admin.id}", update)
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(update, admin.id)
          }
        }

        "update admin with short password" should {
          "return 400" in new AdminsUpdateFSpecContext {
            val update = random[AdminUpdate].copy(password = Some("1234567"))

            Post(s"/v1/admin/admins.update?admin_id=${admin.id}", update)
              .addHeader(adminAuthorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)

              assertErrorCodesAtLeastOnce("InvalidPassword")
            }
          }
        }
      }
    }
    "if request has invalid token" should {
      "be rejected" in new AdminsUpdateFSpecContext {
        val adminId = UUID.randomUUID
        val update = random[AdminUpdate]
        Post(s"/v1/admin/admins.update?admin_id=$adminId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
