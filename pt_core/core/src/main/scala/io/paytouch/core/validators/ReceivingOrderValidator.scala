package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, ReceivingOrderDao }
import io.paytouch.core.data.model.ReceivingOrderRecord
import io.paytouch.core.data.model.enums.ReceivingOrderObjectType
import io.paytouch.core.entities.{ ReceivingOrderUpdate, UserContext }
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils._

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class ReceivingOrderValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[ReceivingOrderRecord] {

  type Record = ReceivingOrderRecord
  type Dao = ReceivingOrderDao

  protected val dao = daos.receivingOrderDao
  val validationErrorF = InvalidReceivingOrderIds(_)
  val accessErrorF = NonAccessibleReceivingOrderIds(_)

  val locationValidator = new LocationValidator
  val purchaseOrderValidator = new PurchaseOrderValidator
  val transferOrderValidator = new TransferOrderValidator

  def validateUpsertion(
      id: UUID,
      upsertion: ReceivingOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ReceivingOrderUpdate]] =
    for {
      validUpdateForSynced <- validateUpdate(id, upsertion)
      validLocation <- locationValidator.accessOneByOptId(upsertion.locationId)
      validReceivingObject <- accessOneReceivingObjectByOptId(
        upsertion.receivingObjectId,
        upsertion.receivingObjectType,
      )
    } yield Multiple.combine(validUpdateForSynced, validLocation, validReceivingObject) { case _ => upsertion }

  def validateSync(id: UUID)(implicit user: UserContext) =
    accessOneById(id).map {
      case Valid(record) if record.synced => Multiple.failure(AlreadySyncedReceivingOrder(id))
      case Valid(record)                  => Multiple.success(record)
      case i @ Invalid(_)                 => i
    }

  private def validateUpdate(
      id: UUID,
      upsertion: ReceivingOrderUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Record]]] =
    validateOneById(id).map {
      case Valid(Some(record)) if !canBeUpdated(record, upsertion) =>
        Multiple.failure(AlreadySyncedReceivingOrderInvalidUpdate(id))
      case validation => validation
    }

  private def canBeUpdated(record: Record, upsertion: ReceivingOrderUpdate): Boolean =
    !record.synced || upsertion == ReceivingOrderUpdate.extractAfterSyncAllowedFields(upsertion)

  private def accessOneReceivingObjectByOptId(
      receivingObjectId: Option[UUID],
      receivingObjectType: Option[ReceivingOrderObjectType],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Unit]]] =
    receivingObjectType match {
      case Some(ReceivingOrderObjectType.PurchaseOrder) =>
        purchaseOrderValidator.accessOneByOptId(receivingObjectId).mapNested(_ => Some((): Unit))
      case Some(ReceivingOrderObjectType.Transfer) =>
        transferOrderValidator.accessOneByOptId(receivingObjectId).mapNested(_ => Some((): Unit))
      case None if receivingObjectId.isDefined =>
        Future.successful(Multiple.failure(ReceivingObjectIdWithoutReceivingObjectType(receivingObjectId.get)))
      case _ => Future.successful(Multiple.empty[Unit])
    }

  override def validateDeletion(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[UUID]]] =
    dao.findByIdsAndMerchantId(ids, user.merchantId).map { records =>
      val syncedRecords = records.filter(_.synced)
      if (syncedRecords.isEmpty) Multiple.success(records.map(_.id))
      else Multiple.failure(AlreadySyncedReceivingOrderInvalidDeletion(syncedRecords.map(_.id)))
    }
}
