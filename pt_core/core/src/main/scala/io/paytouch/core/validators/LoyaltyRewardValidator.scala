package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, LoyaltyRewardDao }
import io.paytouch.core.data.model.LoyaltyRewardRecord
import io.paytouch.core.errors.{ InvalidLoyaltyRewardIds, NonAccessibleLoyaltyRewardIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class LoyaltyRewardValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[LoyaltyRewardRecord] {

  type Record = LoyaltyRewardRecord
  type Dao = LoyaltyRewardDao

  protected val dao = daos.loyaltyRewardDao
  val validationErrorF = InvalidLoyaltyRewardIds(_)
  val accessErrorF = NonAccessibleLoyaltyRewardIds(_)

}
