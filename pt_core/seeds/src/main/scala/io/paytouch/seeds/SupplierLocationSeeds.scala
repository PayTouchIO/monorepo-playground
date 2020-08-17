package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object SupplierLocationSeeds extends Seeds {

  lazy val supplierLocationDao = daos.supplierLocationDao

  def load(
      suppliers: Seq[SupplierRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[SupplierLocationRecord]] = {

    val supplierLocations = suppliers.flatMap { supplier =>
      locations.random(LocationsPerSupplier).map { location =>
        SupplierLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          supplierId = Some(supplier.id),
          locationId = Some(location.id),
          active = None,
        )

      }
    }

    supplierLocationDao.bulkUpsert(supplierLocations).extractRecords
  }
}
