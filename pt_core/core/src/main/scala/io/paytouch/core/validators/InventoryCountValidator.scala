package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._
import cats.data.Validated.{ Invalid, Valid }

import io.paytouch.core.data.daos.{ Daos, InventoryCountDao }
import io.paytouch.core.data.model.InventoryCountRecord
import io.paytouch.core.entities.{ InventoryCountUpdate, UserContext }
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class InventoryCountValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[InventoryCountRecord] {

  type Record = InventoryCountRecord
  type Dao = InventoryCountDao

  protected val dao = daos.inventoryCountDao
  val validationErrorF = InvalidInventoryCountIds(_)
  val accessErrorF = NonAccessibleInventoryCountIds(_)

  val locationValidator = new LocationValidator

  def validateUpsertion(
      upsertion: InventoryCountUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[InventoryCountUpdate]] =
    locationValidator.accessOneById(upsertion.locationId).mapNested(_ => upsertion)

  def validateUpdate(id: UUID)(implicit user: UserContext) =
    accessOneById(id).map {
      case Valid(record) if record.synced => Multiple.failure(AlreadySyncedInventoryCount(id))
      case Valid(record)                  => Multiple.success(record)
      case i @ Invalid(_)                 => i
    }
}
