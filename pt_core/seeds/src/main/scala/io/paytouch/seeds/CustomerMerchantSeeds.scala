package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ ResettableLocalDate, ResettableString }
import io.paytouch.core.entities.enums.CustomerSource

import scala.concurrent._

object CustomerMerchantSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val customerMerchantDao = daos.customerMerchantDao

  def load(customers: Seq[GlobalCustomerRecord])(implicit user: UserRecord): Future[Seq[CustomerMerchantRecord]] = {

    val customerMerchants = customers.random(CustomersPerMerchant).map { customer =>
      CustomerMerchantUpdate(
        _id = None,
        merchantId = Some(user.merchantId),
        customerId = Some(customer.id),
        firstName = customer.firstName,
        lastName = customer.lastName,
        dob = customer.dob,
        anniversary = customer.anniversary,
        email = customer.email,
        phoneNumber = customer.phoneNumber,
        addressLine1 = customer.addressLine1,
        addressLine2 = customer.addressLine2,
        city = customer.city,
        state = customer.state,
        country = customer.country,
        stateCode = customer.stateCode,
        countryCode = customer.countryCode,
        postalCode = customer.postalCode,
        billingDetails = None,
        source = Some(CustomerSource.PtRegister),
      )
    }

    customerMerchantDao.bulkUpsert(customerMerchants).extractRecords
  }
}
