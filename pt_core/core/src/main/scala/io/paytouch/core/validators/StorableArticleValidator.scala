package io.paytouch.core.validators

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors._

import scala.concurrent.ExecutionContext

class StorableArticleValidator(implicit ec: ExecutionContext, daos: Daos) extends ArticleValidator {

  override val validationErrorF = InvalidStorableProductIds(_)
  override val accessErrorF = NonAccessibleStorableProductIds(_)

  override def validityCheck(record: ArticleRecord)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId && record.`type`.isStorable
}
