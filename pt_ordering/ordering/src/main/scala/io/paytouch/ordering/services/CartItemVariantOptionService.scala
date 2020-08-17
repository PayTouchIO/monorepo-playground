package io.paytouch.ordering.services

import io.paytouch.ordering.conversions.CartItemVariantOptionConversions
import io.paytouch.ordering.data.daos.{ CartItemVariantOptionDao, Daos }
import io.paytouch.ordering.data.model.{ CartItemRecord, CartItemVariantOptionRecord }
import io.paytouch.ordering.entities.{ AppContext, CartItemVariantOption => CartItemVariantOptionEntity }
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features.FindExpansionByRecordFeature

import scala.concurrent.{ ExecutionContext, Future }

class CartItemVariantOptionService(implicit val ec: ExecutionContext, val daos: Daos)
    extends CartItemVariantOptionConversions
       with FindExpansionByRecordFeature {

  type Dao = CartItemVariantOptionDao
  type Entity = CartItemVariantOptionEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Record = CartItemVariantOptionRecord

  protected val dao = daos.cartItemVariantOptionDao

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = Seq.empty

  def findItemVariantOptionsByCart(
      cartItemRecords: Seq[CartItemRecord],
    )(implicit
      app: AppContext,
    ): Future[Map[CartItemRecord, Seq[Entity]]] =
    findEntitiesByRecord(cartItemRecords, dao.findByCartItemIds, _.cartItemId)
}
