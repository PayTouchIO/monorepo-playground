package io.paytouch.core.validators

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos.{ Daos, SystemCategoryDao }

import scala.concurrent.ExecutionContext

class SystemCategoryValidator(val ptOrderingClient: PtOrderingClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends GenericCategoryValidator {

  type Dao = SystemCategoryDao

  protected val dao = daos.systemCategoryDao

}
