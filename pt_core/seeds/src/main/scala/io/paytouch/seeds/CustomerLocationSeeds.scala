package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object CustomerLocationSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val customerLocationDao = daos.customerLocationDao

  def load(
      customers: Seq[GlobalCustomerRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[CustomerLocationRecord]] = {

    val customerLocations = customers.flatMap { customer =>
      locations.random(LocationsPerCustomer).map { location =>
        CustomerLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          customerId = Some(customer.id),
          locationId = Some(location.id),
          totalVisits = Some(genInt.instance),
          totalSpendAmount = Some(genBigDecimal.instance),
        )
      }
    }

    customerLocationDao.bulkUpsert(customerLocations).extractRecords
  }

}
