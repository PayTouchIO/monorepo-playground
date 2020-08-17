package io.paytouch.core.validators

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos.{ CatalogCategoryDao, Daos }

import scala.concurrent.ExecutionContext

class CatalogCategoryValidator(
    val ptOrderingClient: PtOrderingClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends GenericCategoryValidator {

  type Dao = CatalogCategoryDao

  protected val dao = daos.catalogCategoryDao

}
