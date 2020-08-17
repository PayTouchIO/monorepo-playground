package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, TransferOrderDao }
import io.paytouch.core.data.model.TransferOrderRecord
import io.paytouch.core.entities.{ TransferOrderUpdate, UserContext }
import io.paytouch.core.errors.{ InvalidTransferOrderIds, NonAccessibleTransferOrderIds }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class TransferOrderValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[TransferOrderRecord] {

  type Record = TransferOrderRecord
  type Dao = TransferOrderDao

  protected val dao = daos.transferOrderDao
  val validationErrorF = InvalidTransferOrderIds(_)
  val accessErrorF = NonAccessibleTransferOrderIds(_)

  val locationValidator = new LocationValidator
  val articleValidator = new ArticleValidator
  val userValidator = new UserValidator

  def validateUpsertion(
      upsertion: TransferOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[TransferOrderUpdate]] = {
    val locationIds = upsertion.fromLocationId.toSeq ++ upsertion.toLocationId.toSeq
    val productIds = upsertion.products.getOrElse(Seq.empty).map(_.productId)
    for {
      validLocations <- locationValidator.accessByIds(locationIds)
      validUser <- userValidator.accessOneByOptId(upsertion.userId)
      validProducts <- articleValidator.accessByIds(productIds)
    } yield Multiple.combine(validLocations, validUser, validProducts) { case _ => upsertion }
  }
}
