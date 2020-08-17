package io.paytouch.core.resources.admin.admins

import java.util.UUID

import com.github.t3hnar.bcrypt._
import io.paytouch.core.conversions.AdminConversions
import io.paytouch.core.data.model.AdminRecord
import io.paytouch.core.entities.{ Admin => AdminEntity, _ }
import io.paytouch.core.utils._

abstract class AdminsFSpec extends FSpec with AdminConversions {

  abstract class AdminResourceFSpecContext extends FSpecContext with AdminFixtures {

    val adminDao = daos.adminDao

    def assertCreation(creation: AdminCreation, id: UUID) =
      assertUpdate(creation.asUpdate, id)

    def assertUpdate(update: AdminUpdate, adminId: UUID) = {
      val record = adminDao.findById(adminId).await.get

      if (update.firstName.isDefined) update.firstName ==== Some(record.firstName)
      if (update.lastName.isDefined) update.lastName ==== Some(record.lastName)
      if (update.email.isDefined) update.email ==== Some(record.email)
      if (update.password.isDefined) update.password.get.isBcrypted(record.password) must beTrue
    }

    def assertResponse(entity: AdminEntity, record: AdminRecord) = {
      entity.id ==== record.id
      entity.firstName ==== record.firstName
      entity.lastName ==== record.lastName
      entity.email ==== record.email
    }

  }

}
