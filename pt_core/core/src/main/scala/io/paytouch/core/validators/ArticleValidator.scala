package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ ArticleDao, Daos }

import scala.concurrent.ExecutionContext

class ArticleValidator(implicit val ec: ExecutionContext, val daos: Daos) extends GenericArticleValidator {

  type Dao = ArticleDao

  val scope = None
  protected val dao = daos.articleDao
}
