package io.paytouch.core.entities

import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.{ MerchantRecord, UserRecord }
import io.paytouch.core.data.model.enums.{ BusinessType, PaymentProcessor }
import io.paytouch.core.entities.enums.{ ContextSource, ExposedName }

final case class UserContext(
    id: UUID,
    merchantId: UUID,
    currency: Currency,
    businessType: BusinessType,
    locationIds: Seq[UUID],
    adminId: Option[UUID] = None,
    merchantSetupCompleted: Boolean,
    source: ContextSource,
    pusherSocketId: Option[String] = None,
    paymentProcessor: PaymentProcessor,
  ) extends ExposedEntity {
  val classShortName = ExposedName.UserContext

  val accessibleLocations: Option[UUID] => Seq[UUID] =
    inputLocationId => accessibleLocations(inputLocationId.map(Seq(_)))

  def accessibleLocations(inputLocationIds: Option[Seq[UUID]]): Seq[UUID] =
    inputLocationIds.fold(locationIds)(inputLIds => locationIds.toSet.intersect(inputLIds.toSet).toSeq)

  val defaultedToUserLocations: Option[UUID] => Seq[UUID] =
    inputLocationId => defaultedToUserLocations(inputLocationId.map(Seq(_)))

  def defaultedToUserLocations(inputLocationIds: Option[Seq[UUID]]): Seq[UUID] =
    inputLocationIds.getOrElse(locationIds)

  def toMerchantContext: MerchantContext =
    MerchantContext(merchantId, currency, pusherSocketId)
}
