package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, UserDao }
import io.paytouch.core.data.model.UserRecord
import io.paytouch.core.errors.{ InvalidUserIds, NonAccessibleUserIds }
import io.paytouch.core.validators.features.DefaultValidatorIncludingDeleted

import scala.concurrent.ExecutionContext

class UserValidatorIncludingDeleted(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidatorIncludingDeleted[UserRecord] {

  type Record = UserRecord
  type Dao = UserDao

  protected val dao = daos.userDao
  val validationErrorF = InvalidUserIds(_)
  val accessErrorF = NonAccessibleUserIds(_)

}
