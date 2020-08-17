package io.paytouch.core.conversions

import io.paytouch.core.calculations.OrderPreperationTimeCalculations
import io.paytouch.core.data.model.{ OnlineOrderAttributeRecord, OnlineOrderAttributeUpdate }
import io.paytouch.core.entities.{ OnlineOrderAttribute, UserContext }
import io.paytouch.core.validators.RecoveredOnlineOrderAttributeUpsertion

trait OnlineOrderAttributeConversions
    extends EntityConversion[OnlineOrderAttributeRecord, OnlineOrderAttribute]
       with OrderPreperationTimeCalculations {

  def fromRecordToEntity(record: OnlineOrderAttributeRecord)(implicit user: UserContext) =
    OnlineOrderAttribute(
      id = record.id,
      acceptanceStatus = record.acceptanceStatus,
      rejectionReason = record.rejectionReason,
      prepareByTime = record.prepareByTime,
      prepareByDateTime = record.prepareByDateTime,
      estimatedPrepTimeInMins = record.estimatedPrepTimeInMins,
      acceptedAt = record.acceptedAt,
      rejectedAt = record.rejectedAt,
      estimatedReadyAt = record.estimatedReadyAt,
      estimatedDeliveredAt = record.estimatedDeliveredAt,
      cancellationStatus = record.cancellationStatus,
      cancellationReason = record.cancellationReason,
    )

  protected def toUpdate(onlineOrderAttribute: RecoveredOnlineOrderAttributeUpsertion)(implicit user: UserContext) =
    OnlineOrderAttributeUpdate(
      id = Some(onlineOrderAttribute.id),
      merchantId = Some(user.merchantId),
      acceptanceStatus = onlineOrderAttribute.acceptanceStatus,
      rejectionReason = None,
      prepareByTime = onlineOrderAttribute.prepareByTime,
      prepareByDateTime = onlineOrderAttribute.prepareByDateTime,
      estimatedPrepTimeInMins = onlineOrderAttribute.estimatedPrepTimeInMins,
      acceptedAt = None,
      rejectedAt = None,
      estimatedReadyAt = None,
      estimatedDeliveredAt = None,
      cancellationStatus = onlineOrderAttribute.cancellationStatus,
      cancellationReason = onlineOrderAttribute.cancellationReason,
    )
}
