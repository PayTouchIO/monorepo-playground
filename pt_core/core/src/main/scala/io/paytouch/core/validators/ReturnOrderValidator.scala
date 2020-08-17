package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.{ Daos, ReturnOrderDao }
import io.paytouch.core.data.model.ReturnOrderRecord
import io.paytouch.core.entities.{ ReturnOrderUpdate, UserContext }
import io.paytouch.core.errors.{ AlreadySyncedReturnOrder, InvalidReturnOrderIds, NonAccessibleReturnOrderIds }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class ReturnOrderValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[ReturnOrderRecord] {

  type Record = ReturnOrderRecord
  type Dao = ReturnOrderDao

  protected val dao = daos.returnOrderDao
  val validationErrorF = InvalidReturnOrderIds(_)
  val accessErrorF = NonAccessibleReturnOrderIds(_)

  val locationValidator = new LocationValidator
  val articleValidator = new ArticleValidator
  val supplierValidator = new SupplierValidator
  val userValidator = new UserValidator
  val purchaseOrderValidator = new PurchaseOrderValidator

  def validateUpsertion(
      upsertion: ReturnOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ReturnOrderUpdate]] = {
    val productIds = upsertion.products.getOrElse(Seq.empty).map(_.productId)
    for {
      validLocation <- locationValidator.accessOneByOptId(upsertion.locationId)
      validUser <- userValidator.accessOneByOptId(upsertion.userId)
      validSupplier <- supplierValidator.accessOneByOptId(upsertion.supplierId)
      validProducts <- articleValidator.accessByIds(productIds)
      validPurchaseOrder <- purchaseOrderValidator.accessOneByOptId(upsertion.purchaseOrderId)
    } yield Multiple.combine(validLocation, validUser, validSupplier, validProducts, validPurchaseOrder) {
      case _ => upsertion
    }
  }

  def validateUpdate(id: UUID)(implicit user: UserContext) =
    accessOneById(id).map {
      case Valid(record) if record.synced => Multiple.failure(AlreadySyncedReturnOrder(id))
      case Valid(record)                  => Multiple.success(record)
      case i @ Invalid(_)                 => i
    }
}
