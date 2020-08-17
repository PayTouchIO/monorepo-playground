package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, ProductDao }
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.errors.{ InvalidProductIds, NonAccessibleProductIds }
import io.paytouch.core.validators.features.DefaultValidatorIncludingDeleted

import scala.concurrent.ExecutionContext

class ProductValidatorIncludingDeleted(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidatorIncludingDeleted[ArticleRecord] {

  type Record = ArticleRecord
  type Dao = ProductDao

  protected val dao = daos.productDao
  val scope = Some(ArticleScope.Product)

  val validationErrorF = InvalidProductIds(_, scope)
  val accessErrorF = NonAccessibleProductIds(_, scope)

}
