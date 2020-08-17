package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.{ Daos, PurchaseOrderDao }
import io.paytouch.core.data.model.PurchaseOrderRecord
import io.paytouch.core.entities.{ PurchaseOrderUpdate, UserContext }
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class PurchaseOrderValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[PurchaseOrderRecord] {

  type Record = PurchaseOrderRecord
  type Dao = PurchaseOrderDao

  protected val dao = daos.purchaseOrderDao
  val validationErrorF = InvalidPurchaseOrderIds(_)
  val accessErrorF = NonAccessiblePurchaseOrderIds(_)

  val supplierValidator = new SupplierValidator
  val locationValidator = new LocationValidator

  def validateUpsertion(
      id: UUID,
      upsertion: PurchaseOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[PurchaseOrderUpdate]] =
    for {
      validUpdateForSent <- validateUpdate(id, upsertion)
      validSupplier <- supplierValidator.accessOneByOptId(upsertion.supplierId)
      validLocation <- locationValidator.accessOneByOptId(upsertion.locationId)
    } yield Multiple.combine(validUpdateForSent, validSupplier, validLocation) { case _ => upsertion }

  private def validateUpdate(
      id: UUID,
      upsertion: PurchaseOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Record]]] =
    validateOneById(id).map {
      case Valid(Some(record)) if record.sent =>
        Multiple.failure(AlreadySentPurchaseOrder(id))
      case validation => validation
    }

  def validateSend(id: UUID)(implicit user: UserContext) =
    accessOneById(id).map {
      case Valid(record) if record.sent => Multiple.failure(AlreadySentPurchaseOrder(id))
      case Valid(record)                => Multiple.success(record)
      case i @ Invalid(_)               => i
    }

  override def validateDeletion(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[UUID]]] =
    dao.findByIdsAndMerchantId(ids, user.merchantId).map { records =>
      val sentRecords = records.filter(_.sent)
      if (sentRecords.isEmpty) Multiple.success(records.map(_.id))
      else Multiple.failure(AlreadySentPurchaseOrderInvalidDeletion(sentRecords.map(_.id)))
    }
}
