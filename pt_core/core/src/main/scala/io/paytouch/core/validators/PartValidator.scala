package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, PartDao }
import io.paytouch.core.data.model.enums.ArticleScope

import scala.concurrent.ExecutionContext

class PartValidator(implicit val ec: ExecutionContext, val daos: Daos) extends GenericArticleValidator {

  type Dao = PartDao

  val scope = Some(ArticleScope.Part)
  protected val dao = daos.partDao
}
