package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, GiftCardPassTransactionDao }
import io.paytouch.core.data.model.GiftCardPassTransactionRecord
import io.paytouch.core.errors._
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class GiftCardPassTransactionValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[GiftCardPassTransactionRecord] {

  type Record = GiftCardPassTransactionRecord
  type Dao = GiftCardPassTransactionDao

  protected val dao = daos.giftCardPassTransactionDao
  val validationErrorF = InvalidGiftCardPassTransactionIds(_)
  val accessErrorF = NonAccessibleGiftCardPassTransactionIds(_)
}
