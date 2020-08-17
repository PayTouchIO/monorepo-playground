package io.paytouch.core.validators

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.daos.{ Daos, KitchenDao }
import io.paytouch.core.data.model.KitchenRecord
import io.paytouch.core.entities.{ KitchenUpdate, UserContext }
import io.paytouch.core.errors.{
  InvalidKitchenIds,
  InvalidLocationIdChange,
  KitchenStillInUse,
  NonAccessibleKitchenIds,
}
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils._
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class KitchenValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[KitchenRecord]
       with LazyLogging {
  type Record = KitchenRecord
  type Update = KitchenUpdate
  type Dao = KitchenDao

  protected val dao = daos.kitchenDao
  val validationErrorF = InvalidKitchenIds(_)
  val accessErrorF = NonAccessibleKitchenIds(_)

  val locationValidator = new LocationValidator
  val productLocationValidator = new ProductLocationValidator

  def validateUpsertion(id: UUID, update: Update)(implicit user: UserContext): Future[ErrorsOr[Update]] =
    for {
      existingKitchen <- dao.findById(id)
      locationId = update.locationId.orElse(existingKitchen.map(_.locationId)).getOrElse(UUID.randomUUID)
      location <- locationValidator.accessOneById(locationId)
      locationIdNotChanged <- validateLocationIdNotChanged(existingKitchen, locationId)
    } yield Multiple.combine(location, locationIdNotChanged) {
      case _ => update
    }

  private def validateLocationIdNotChanged(
      existing: Option[Record],
      locationId: UUID,
    ): Future[ErrorsOr[Option[UUID]]] =
    Future.successful {
      (existing, locationId) match {
        case (Some(record), lId) if lId == record.locationId => Multiple.successOpt(locationId)
        case (Some(_), changedLocationId)                    => Multiple.failure(InvalidLocationIdChange(changedLocationId))
        case _                                               => Multiple.successOpt(locationId)
      }
    }

  def accessOneByNameCaseInsensitive(
      name: String,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Record]] =
    dao.findByNamesAndMerchantId(Seq(name), user.merchantId, locationId).map {
      case records =>
        records.headOption match {
          case Some(record) => Multiple.success(record)
          case _            => Multiple.failure(accessErrorF(Seq.empty))
        }
    }

  override def validateDeletion(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[UUID]]] =
    productLocationValidator.findByRoutingToKitchenId(ids).map { result =>
      if (result.isEmpty) Multiple.success(ids)
      else {
        val routeToKitchenIds = result.flatMap(_.routeToKitchenId).toSet
        val diff = routeToKitchenIds diff ids.toSet
        Multiple.failure(KitchenStillInUse(diff.toSeq))
      }
    }
}
