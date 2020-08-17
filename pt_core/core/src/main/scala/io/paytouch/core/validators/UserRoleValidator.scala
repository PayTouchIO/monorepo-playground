package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, UserRoleDao }
import io.paytouch.core.data.model.UserRoleRecord
import io.paytouch.core.errors.{ InvalidUserRoleIds, NonAccessibleUserRoleIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class UserRoleValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[UserRoleRecord] {

  type Record = UserRoleRecord
  type Dao = UserRoleDao

  protected val dao = daos.userRoleDao
  val validationErrorF = InvalidUserRoleIds(_)
  val accessErrorF = NonAccessibleUserRoleIds(_)
}
