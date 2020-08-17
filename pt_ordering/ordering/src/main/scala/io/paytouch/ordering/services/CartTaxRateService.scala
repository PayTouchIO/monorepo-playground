package io.paytouch.ordering
package services

import io.paytouch.ordering.conversions.CartTaxRateConversions
import io.paytouch.ordering.data.daos.{ CartTaxRateDao, Daos }
import io.paytouch.ordering.data.model.{ CartRecord, CartTaxRateRecord }
import io.paytouch.ordering.entities.{ AppContext, CartTaxRate => CartTaxRateEntity }
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features.FindExpansionByRecordFeature

import scala.concurrent.{ ExecutionContext, Future }

class CartTaxRateService(implicit val ec: ExecutionContext, val daos: Daos)
    extends CartTaxRateConversions
       with FindExpansionByRecordFeature {

  type Dao = CartTaxRateDao
  type Entity = CartTaxRateEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Record = CartTaxRateRecord

  protected val dao = daos.cartTaxRateDao

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = Seq.empty

  def findTaxRatesByCart(
      cartRecords: Seq[CartRecord],
    )(implicit
      context: AppContext,
    ): Future[Map[CartRecord, Seq[Entity]]] =
    findEntitiesByRecord(cartRecords, dao.findByCartIds, _.cartId)
}
