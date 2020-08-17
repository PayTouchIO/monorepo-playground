package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object TaxRateLocationSeeds extends Seeds {

  lazy val taxRateLocationDao = daos.taxRateLocationDao

  def load(
      taxRates: Seq[TaxRateRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[TaxRateLocationRecord]] = {
    import SeedsQuantityProvider._

    val taxRateLocations = taxRates.flatMap { taxRate =>
      locations.random(LocationsPerTaxRate).map { location =>
        TaxRateLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          taxRateId = Some(taxRate.id),
          locationId = Some(location.id),
          active = None,
        )
      }
    }
    taxRateLocationDao.bulkUpsertByRelIds(taxRateLocations).extractRecords
  }
}
