package io.paytouch.core.data.model

import io.paytouch.core.utils.PaytouchSpec
import org.specs2.specification.Scope

class PermissionSpec extends PaytouchSpec {

  abstract class PermissionSpecContext extends Scope {

    def assertPermission(p: Permission) =
      Permission.fromRepresentation(p.representation) ==== p
  }

  "Permission" should {

    "be serializable and deserializable" in new PermissionSpecContext {
      assertPermission(Permission())
      assertPermission(Permission(read = true))
      assertPermission(Permission(read = true, create = true))
      assertPermission(Permission(read = true, edit = true))
      assertPermission(Permission(read = true, create = true, edit = true))
      assertPermission(Permission(read = true, create = true, edit = true, delete = true))
    }
  }

}
