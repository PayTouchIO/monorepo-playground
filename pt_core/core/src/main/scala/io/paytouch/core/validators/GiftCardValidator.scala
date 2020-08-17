package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, GiftCardDao }
import io.paytouch.core.data.model.GiftCardRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{ GiftCardUniquePerMerchant, InvalidGiftCardIds, NonAccessibleGiftCardIds }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class GiftCardValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[GiftCardRecord] {

  type Record = GiftCardRecord
  type Dao = GiftCardDao

  protected val dao = daos.giftCardDao
  val validationErrorF = InvalidGiftCardIds(_)
  val accessErrorF = NonAccessibleGiftCardIds(_)

  def validateUpsertion(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Option[GiftCardRecord]]] =
    for {
      validRecordById <- validateOneById(id)
      validNoMoreThanOne <- validateNoMoreThanOne(id)
    } yield Multiple.combine(validRecordById, validNoMoreThanOne) { case (record, _) => record }

  private def validateNoMoreThanOne(
      id: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[GiftCardRecord]]] =
    dao.findAllByMerchantId(user.merchantId).map { records =>
      val atLeastAnotherOne = records.exists(_.id != id)
      if (atLeastAnotherOne) Multiple.failure(GiftCardUniquePerMerchant(records.map(_.id)))
      else Multiple.empty
    }
}
