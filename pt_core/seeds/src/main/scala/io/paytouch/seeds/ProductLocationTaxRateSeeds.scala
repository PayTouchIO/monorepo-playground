package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ProductLocationTaxRateSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val productLocationTaxRateDao = daos.productLocationTaxRateDao

  def load(
      taxRateLocations: Seq[TaxRateLocationRecord],
      productLocations: Seq[ProductLocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductLocationTaxRateRecord]] = {

    val productLocationTaxRates = taxRateLocations.flatMap { taxRatelocation =>
      productLocations.filter(_.locationId == taxRatelocation.locationId).random(ProductsPerTaxRate).map {
        productLocation =>
          ProductLocationTaxRateUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            productLocationId = Some(productLocation.id),
            taxRateId = Some(taxRatelocation.taxRateId),
          )
      }
    }

    productLocationTaxRateDao.bulkUpsertByRelIds(productLocationTaxRates).extractRecords
  }
}
