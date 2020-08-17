package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, GroupDao }
import io.paytouch.core.data.model.GroupRecord
import io.paytouch.core.errors.{ InvalidGroupIds, NonAccessibleGroupIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class GroupValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[GroupRecord] {

  type Record = GroupRecord
  type Dao = GroupDao

  protected val dao = daos.groupDao
  val validationErrorF = InvalidGroupIds(_)
  val accessErrorF = NonAccessibleGroupIds(_)
}
