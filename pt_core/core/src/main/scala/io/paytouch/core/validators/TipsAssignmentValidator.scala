package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, TipsAssignmentDao }
import io.paytouch.core.data.model.TipsAssignmentRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{ InvalidTipsAssignmentIds, NonAccessibleTipsAssignmentIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class TipsAssignmentValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[TipsAssignmentRecord] {

  type Record = TipsAssignmentRecord
  type Dao = TipsAssignmentDao

  protected val dao = daos.tipsAssignmentDao
  val validationErrorF = InvalidTipsAssignmentIds(_)
  val accessErrorF = NonAccessibleTipsAssignmentIds(_)

  override protected def validityCheck(record: TipsAssignmentRecord)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId
}
