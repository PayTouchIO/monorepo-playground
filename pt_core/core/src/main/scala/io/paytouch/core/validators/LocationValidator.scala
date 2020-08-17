package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.data.daos.{ Daos, LocationDao }
import io.paytouch.core.data.model.LocationRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors._
import io.paytouch.core.utils._
import io.paytouch.core.validators.features._

class LocationValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[LocationRecord]
       with EmailValidator {
  type Record = LocationRecord
  type Dao = LocationDao

  protected val dao = daos.locationDao
  val validationErrorF = InvalidLocationIds(_)
  val accessErrorF = NonAccessibleLocationIds(_)

  override def validityCheck(record: LocationRecord)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId && user.locationIds.contains(record.id)

  // temporary solution to relax following requirement in user upsertion:
  // people can only associate other people to locations they belong to
  def validateBelongsToMerchant(
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Multiple.ErrorsOr[Seq[LocationRecord]]] =
    dao.findByIdsAndMerchantId(locationIds, user.merchantId).map { records =>
      val retrievedIds = records.map(_.id)
      val diff = locationIds.toSet diff retrievedIds.toSet

      if (diff.isEmpty)
        Multiple.success(records)
      else
        Multiple.failure(accessErrorF(diff.toSeq))
    }
}
