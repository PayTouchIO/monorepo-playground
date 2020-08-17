package io.paytouch.seeds

import scala.concurrent._

import io.paytouch.core.data.model._
import io.paytouch.seeds.IdsProvider._

object GlobalCustomerSeeds extends Seeds {
  lazy val globalCustomerDao = daos.globalCustomerDao

  def load(implicit user: UserRecord): Future[Seq[GlobalCustomerRecord]] = {
    val customerIds = customersPerEmail(user.email)

    val customers = customerIds.map { customerId =>
      val firstName = randomWords
      val lastName = randomWords
      val address = genAddress.instance
      GlobalCustomerUpdate(
        id = Some(customerId),
        firstName = Some(firstName),
        lastName = Some(lastName),
        dob = None,
        anniversary = None,
        email = Some(s"$firstName.$lastName@$randomWord.com".replace(" ", "_")),
        phoneNumber = None,
        addressLine1 = address.line1,
        addressLine2 = address.line2,
        city = address.city,
        state = address.state,
        country = address.country,
        stateCode = address.stateData.map(_.code),
        countryCode = address.countryData.map(_.code),
        postalCode = address.postalCode,
        mobileStorefrontLastLogin = None,
        webStorefrontLastLogin = None,
      )
    }

    globalCustomerDao.bulkUpsert(customers).extractRecords
  }
}
