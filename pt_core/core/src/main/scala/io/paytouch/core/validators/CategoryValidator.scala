package io.paytouch.core.validators

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos.{ CategoryDao, Daos }

import scala.concurrent.ExecutionContext

class CategoryValidator(val ptOrderingClient: PtOrderingClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends GenericCategoryValidator {

  type Dao = CategoryDao

  protected val dao = daos.categoryDao

}
