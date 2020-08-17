package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object CustomerGroupSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val customerGroupDao = daos.customerGroupDao

  def load(
      customers: Seq[GlobalCustomerRecord],
      groups: Seq[GroupRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[CustomerGroupRecord]] = {

    val customerGroups = groups.flatMap { group =>
      customers.random(CustomersPerGroup).map { customer =>
        CustomerGroupUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          customerId = Some(customer.id),
          groupId = Some(group.id),
        )
      }
    }

    customerGroupDao.bulkUpsert(customerGroups).extractRecords
  }
}
