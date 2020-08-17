package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, ProductDao }
import io.paytouch.core.data.model.enums.ArticleScope

import scala.concurrent.ExecutionContext

class ProductValidator(implicit val ec: ExecutionContext, val daos: Daos) extends GenericArticleValidator {

  type Dao = ProductDao

  val scope = Some(ArticleScope.Product)
  protected val dao = daos.productDao
}
