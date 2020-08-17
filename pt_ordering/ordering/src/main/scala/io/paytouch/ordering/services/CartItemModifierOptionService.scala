package io.paytouch.ordering.services

import io.paytouch.ordering.conversions.CartItemModifierOptionConversions
import io.paytouch.ordering.data.daos.{ CartItemModifierOptionDao, Daos }
import io.paytouch.ordering.data.model.{ CartItemModifierOptionRecord, CartItemRecord }
import io.paytouch.ordering.entities.{ AppContext, CartItemModifierOption => CartItemModifierOptionEntity }
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features.FindExpansionByRecordFeature

import scala.concurrent.{ ExecutionContext, Future }

class CartItemModifierOptionService(implicit val ec: ExecutionContext, val daos: Daos)
    extends CartItemModifierOptionConversions
       with FindExpansionByRecordFeature {

  type Dao = CartItemModifierOptionDao
  type Entity = CartItemModifierOptionEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Record = CartItemModifierOptionRecord

  protected val dao = daos.cartItemModifierOptionDao

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = Seq.empty

  def findItemModifierOptionsByCart(
      cartItemRecords: Seq[CartItemRecord],
    )(implicit
      context: AppContext,
    ): Future[Map[CartItemRecord, Seq[Entity]]] =
    findEntitiesByRecord(cartItemRecords, dao.findByCartItemIds, _.cartItemId)
}
