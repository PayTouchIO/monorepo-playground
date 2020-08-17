package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, LoyaltyProgramDao }
import io.paytouch.core.data.model.LoyaltyProgramRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{
  InvalidLoyaltyProgramIds,
  LoyaltyProgramUniquePerMerchant,
  NonAccessibleLoyaltyProgramIds,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class LoyaltyProgramValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[LoyaltyProgramRecord] {

  type Record = LoyaltyProgramRecord
  type Dao = LoyaltyProgramDao

  protected val dao = daos.loyaltyProgramDao
  val validationErrorF = InvalidLoyaltyProgramIds(_)
  val accessErrorF = NonAccessibleLoyaltyProgramIds(_)

  def validateNoMoreThanOne(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Option[LoyaltyProgramRecord]]] =
    dao.findAllByMerchantId(user.merchantId, None, None)(offset = 0, limit = 2).map { records =>
      val atLeastAnotherOne = records.exists(_.id != id)
      if (atLeastAnotherOne) Multiple.failure(LoyaltyProgramUniquePerMerchant(records.map(_.id)))
      else Multiple.empty
    }
}
