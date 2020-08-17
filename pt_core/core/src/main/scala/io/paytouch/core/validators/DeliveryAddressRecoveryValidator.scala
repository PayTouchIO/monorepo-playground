package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.{ Daos, OrderDeliveryAddressDao }
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import io.paytouch.core.validators.features.DefaultRecoveryValidator

import scala.concurrent._

class DeliveryAddressRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[OrderDeliveryAddressRecord] {

  val deliveryAddressValidator = new OrderDeliveryAddressValidator
  type Record = OrderDeliveryAddressRecord
  type Dao = OrderDeliveryAddressDao

  protected val dao = daos.orderDeliveryAddressDao
  val validationErrorF = deliveryAddressValidator.validationErrorF
  val accessErrorF = deliveryAddressValidator.accessErrorF

  def validateUpsertion(
      maybeUpsertion: Option[DeliveryAddressUpsertion],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[DeliveryAddressUpsertion]]] =
    maybeUpsertion match {
      case None => Future.successful(Multiple.empty)
      case Some(upsertion) =>
        val deliveryAddressIds = Seq(upsertion.id)
        deliveryAddressValidator.filterNonAlreadyTakenIds(deliveryAddressIds).map { validAddressIds =>
          recoverAddressId(validAddressIds, upsertion.id) match {
            case Valid(_)       => Multiple.successOpt(upsertion)
            case i @ Invalid(_) => i
          }
        }
    }

  def recoverUpsertion(
      upsertion: Option[DeliveryAddressUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Option[RecoveredDeliveryAddressUpsertion]] =
    upsertion.fold[Future[Option[RecoveredDeliveryAddressUpsertion]]](Future.successful(None))(recoverUpsertion)

  private def recoverUpsertion(
      upsertion: DeliveryAddressUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[RecoveredDeliveryAddressUpsertion]] = {
    val deliveryAddressIds = Seq(upsertion.id)
    deliveryAddressValidator.filterNonAlreadyTakenIds(deliveryAddressIds).map { validAddressIds =>
      val recoveredAddressId = logger.loggedSoftRecoverUUID(recoverAddressId(validAddressIds, upsertion.id))(
        "While recovering delivery address id not accessible",
      )
      Some(toRecoveredDeliveryAddressUpsertion(recoveredAddressId, upsertion))
    }
  }

  private def recoverAddressId(addressIds: Seq[UUID], addressId: UUID): ErrorsOr[UUID] =
    if (addressIds.contains(addressId)) Multiple.success(addressId)
    else Multiple.failure(accessErrorF(Seq(addressId)))

  private def toRecoveredDeliveryAddressUpsertion(
      recoveredAddressId: UUID,
      upsertion: DeliveryAddressUpsertion,
    ): RecoveredDeliveryAddressUpsertion =
    RecoveredDeliveryAddressUpsertion(
      id = recoveredAddressId,
      firstName = upsertion.firstName,
      lastName = upsertion.lastName,
      address = upsertion.address,
      drivingDistanceInMeters = upsertion.drivingDistanceInMeters,
      estimatedDrivingTimeInMins = upsertion.estimatedDrivingTimeInMins,
    )
}

final case class RecoveredDeliveryAddressUpsertion(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
    address: AddressSync = AddressSync.empty,
    drivingDistanceInMeters: ResettableBigDecimal,
    estimatedDrivingTimeInMins: ResettableInt,
  )
