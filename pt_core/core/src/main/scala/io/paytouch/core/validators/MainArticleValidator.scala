package io.paytouch.core.validators

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors._

import scala.concurrent.ExecutionContext

class MainArticleValidator(implicit ec: ExecutionContext, daos: Daos) extends ArticleValidator {

  override val validationErrorF = InvalidMainProductIds(_)
  override val accessErrorF = NonAccessibleMainProductIds(_)

  override def validityCheck(record: ArticleRecord)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId && record.`type`.isMain
}
