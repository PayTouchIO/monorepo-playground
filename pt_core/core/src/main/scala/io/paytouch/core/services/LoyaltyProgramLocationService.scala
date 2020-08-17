package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.LoyaltyProgramLocationConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{
  LoyaltyProgramLocationRecord,
  LoyaltyProgramLocationUpdate => LoyaltyProgramLocationUpdateModel,
}
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.{ LocationValidator, LoyaltyProgramValidator }

import scala.concurrent._

class LoyaltyProgramLocationService(implicit val ec: ExecutionContext, val daos: Daos)
    extends LoyaltyProgramLocationConversions {

  protected val dao = daos.loyaltyProgramLocationDao

  val loyaltyProgramValidator = new LoyaltyProgramValidator
  val locationValidator = new LocationValidator

  def findAllByLoyaltyProgramIds(
      loyaltyProgramIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Seq[LoyaltyProgramLocationRecord]] =
    dao.findByItemIdsAndLocationIds(loyaltyProgramIds, user.locationIds)

  def accessItemById(id: UUID)(implicit user: UserContext) = loyaltyProgramValidator.accessOneById(id)

  def convertToLoyaltyProgramLocationUpdates(
      userId: UUID,
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[LoyaltyProgramLocationUpdateModel]]] =
    locationIds match {
      case Some(locIds) => convertToLoyaltyProgramLocationUpdates(userId, locIds)
      case _            => Future.successful(Multiple.success(Seq.empty))
    }

  def convertToLoyaltyProgramLocationUpdates(
      itemId: UUID,
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[LoyaltyProgramLocationUpdateModel]]] =
    locationValidator.accessByIds(locationIds).mapNested(_ => toLoyaltyProgramLocationUpdates(itemId, locationIds))
}
