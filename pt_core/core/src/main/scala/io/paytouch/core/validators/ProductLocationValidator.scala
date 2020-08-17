package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ProductLocationRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.InvalidProductLocationAssociation
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class ProductLocationValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  protected val dao = daos.productLocationDao

  def validateProductLocationsByRelationIds(
      ids: Seq[(UUID, UUID)],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[ProductLocationRecord]]] = {
    val nonDupIds = ids.distinct
    val productIds = nonDupIds.map { case (p, _) => p }
    val locationIds = nonDupIds.map { case (_, l) => l }
    dao.findByItemIdsAndLocationIds(productIds, locationIds).map { productLocations =>
      val areValid = nonDupIds.forall { case (pId, lId) => isValidProductLocation(pId, lId, productLocations) }
      if (!areValid) {
        val invalidRelIds = nonDupIds.filterNot {
          case (pId, lId) => isValidProductLocation(pId, lId, productLocations)
        }
        Multiple.failure(InvalidProductLocationAssociation(invalidRelIds))
      }
      else Multiple.success(productLocations)
    }
  }

  private def isValidProductLocation(
      productId: UUID,
      locationId: UUID,
      productLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserContext,
    ): Boolean =
    productLocations.exists { productLocation =>
      productLocation.contains(productId, locationId) &&
      productLocation.merchantId == user.merchantId
    }

  def findByRoutingToKitchenId(kitchenIds: Seq[UUID]): Future[Seq[ProductLocationRecord]] =
    dao.findByRoutingToKitchenId(kitchenIds)
}
