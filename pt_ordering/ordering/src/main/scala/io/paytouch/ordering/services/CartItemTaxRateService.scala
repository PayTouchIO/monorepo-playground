package io.paytouch.ordering.services

import io.paytouch.ordering.conversions.CartItemTaxRateConversions
import io.paytouch.ordering.data.daos.{ CartItemTaxRateDao, Daos }
import io.paytouch.ordering.data.model.{ CartItemRecord, CartItemTaxRateRecord }
import io.paytouch.ordering.entities.{ AppContext, CartItemTaxRate => CartItemTaxRateEntity }
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features.FindExpansionByRecordFeature

import scala.concurrent.{ ExecutionContext, Future }

class CartItemTaxRateService(implicit val ec: ExecutionContext, val daos: Daos)
    extends CartItemTaxRateConversions
       with FindExpansionByRecordFeature {

  type Dao = CartItemTaxRateDao
  type Entity = CartItemTaxRateEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Record = CartItemTaxRateRecord

  protected val dao = daos.cartItemTaxRateDao

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = Seq.empty

  def findItemTaxRatesByCart(
      cartItemRecords: Seq[CartItemRecord],
    )(implicit
      app: AppContext,
    ): Future[Map[CartItemRecord, Seq[Entity]]] =
    findEntitiesByRecord(cartItemRecords, dao.findByCartItemIds, _.cartItemId)
}
