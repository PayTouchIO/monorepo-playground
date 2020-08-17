package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ CommentDao, Daos }
import io.paytouch.core.data.model.CommentRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{ InvalidCommentIds, NonAccessibleCommentIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class CommentValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[CommentRecord] {

  type Record = CommentRecord
  type Dao = CommentDao

  protected val dao = daos.commentDao
  val validationErrorF = InvalidCommentIds(_)
  val accessErrorF = NonAccessibleCommentIds(_)

  override protected def validityCheck(record: CommentRecord)(implicit user: UserContext): Boolean =
    (record.merchantId == user.merchantId) && (record.userId == user.id)
}
