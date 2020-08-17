package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, LoyaltyMembershipDao }
import io.paytouch.core.data.model.LoyaltyMembershipRecord
import io.paytouch.core.errors.{ InvalidLoyaltyMembershipIds, NonAccessibleLoyaltyMembershipIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class LoyaltyMembershipValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[LoyaltyMembershipRecord] {

  type Record = LoyaltyMembershipRecord
  type Dao = LoyaltyMembershipDao

  protected val dao = daos.loyaltyMembershipDao
  val validationErrorF = InvalidLoyaltyMembershipIds(_)
  val accessErrorF = NonAccessibleLoyaltyMembershipIds(_)

}
