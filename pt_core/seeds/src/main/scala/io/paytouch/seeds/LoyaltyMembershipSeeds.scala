package io.paytouch.seeds

import io.paytouch.core.data.model._
import org.scalacheck.Gen

import scala.concurrent._

object LoyaltyMembershipSeeds extends Seeds {

  lazy val loyaltyMembershipDao = daos.loyaltyMembershipDao

  def load(
      loyaltyPrograms: Seq[LoyaltyProgramRecord],
      customerMerchants: Seq[CustomerMerchantRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[LoyaltyMembershipRecord]] = {

    val loyaltyMemberships = loyaltyPrograms.flatMap { loyaltyProgram =>
      customerMerchants.randomAtLeast(10).map { customerMerchant =>
        LoyaltyMembershipUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          customerId = Some(customerMerchant.customerId),
          loyaltyProgramId = Some(loyaltyProgram.id),
          lookupId = Some(genLookupId.instance),
          iosPassPublicUrl = Gen.option(genWord).instance.map(w => s"https://ios.pass.$w/$w"),
          androidPassPublicUrl = Gen.option(genWord).instance.map(w => s"https://android.pass.$w/$w"),
          points = genOptInt.instance,
          customerOptInAt = None,
          merchantOptInAt = None,
        )
      }
    }

    loyaltyMembershipDao.bulkUpsert(loyaltyMemberships).extractRecords
  }
}
