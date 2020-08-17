package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.LocationSettingsConversions
import io.paytouch.core.data.daos.{ Daos, LocationSettingsDao }
import io.paytouch.core.data.model.LocationSettingsRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{ InvalidLocationIds, NonAccessibleLocationIds }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class LocationSettingsValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[LocationSettingsRecord]
       with LocationSettingsConversions {

  type Record = LocationSettingsRecord
  type Dao = LocationSettingsDao

  protected val dao = daos.locationSettingsDao
  val validationErrorF = InvalidLocationIds(_)
  val accessErrorF = NonAccessibleLocationIds(_)

  val locationValidator = new LocationValidator

  override def accessOneById(id: UUID)(implicit user: UserContext): Future[ErrorsOr[LocationSettingsRecord]] =
    locationValidator.accessOneById(id).flatMapTraverse { _ =>
      val merchantId = user.merchantId
      dao.findByLocationId(id, merchantId).flatMap {
        case Some(ls) => Future.successful(ls)
        case None =>
          for {
            latestSettings <- dao.findLatestSettings(merchantId)
            default = toDefaultLocationSettings(id, latestSettings)
            (_, record) <- dao.upsert(default)
          } yield record
      }
    }
}
