package io.paytouch.ordering.services.features

import java.util.UUID

import io.paytouch.ordering.data.daos.features.SlickStoreDao
import io.paytouch.ordering.entities.StoreContext

import scala.concurrent.Future

trait DefaultDeleteStoreFeature extends DeleteFeature { self =>

  type Context = StoreContext
  type Dao <: SlickStoreDao

  protected def deleteByIds(ids: Seq[UUID])(implicit store: StoreContext): Future[Seq[UUID]] = {
    val storeIds = Seq(store.id)
    dao.deleteByIdsAndStoreIds(ids = ids, storeIds = storeIds)
  }

}
