package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.TaxRateLocationRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.InvalidTaxRateLocationAssociation
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class TaxRateLocationValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  protected val dao = daos.taxRateLocationDao

  def accessLocationAndTaxRatesByIds(
      locationToTaxRateIds: Map[UUID, Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[TaxRateLocationRecord]]] = {
    val locationIds = locationToTaxRateIds.keys.toSeq
    val taxRateIds = locationToTaxRateIds.values.flatten.toSeq
    dao.findByLocationIdsAndTaxRateIds(locationIds, taxRateIds).map { records =>
      val areValid = locationToTaxRateIds.forall {
        case (locationId, taxRateIds) =>
          taxRateIds.forall(taxRateId => isValidTaxRateLocation(locationId, taxRateId, records))
      }
      if (areValid) Multiple.success(records)
      else Multiple.failure(InvalidTaxRateLocationAssociation())
    }
  }

  private def isValidTaxRateLocation(
      locationId: UUID,
      taxRateId: UUID,
      taxRateLocations: Seq[TaxRateLocationRecord],
    )(implicit
      user: UserContext,
    ) =
    taxRateLocations.exists { taxRateLocation =>
      taxRateLocation.locationId == locationId && taxRateLocation.taxRateId == taxRateId &&
      taxRateLocation.merchantId == user.merchantId
    }
}
