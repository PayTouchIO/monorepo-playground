package io.paytouch.core.conversions

import io.paytouch.core.data.model.TipsAssignmentRecord
import io.paytouch.core.entities.{ MonetaryAmount, UserContext, TipsAssignment => TipsAssignmentEntity }

trait TipsAssignmentConversions extends EntityConversion[TipsAssignmentRecord, TipsAssignmentEntity] {

  def fromRecordToEntity(record: TipsAssignmentRecord)(implicit user: UserContext): TipsAssignmentEntity =
    TipsAssignmentEntity(
      id = record.id,
      locationId = record.locationId,
      userId = record.userId,
      orderId = record.orderId,
      amount = MonetaryAmount(record.amount),
      handledVia = record.handledVia,
      handledViaCashDrawerActivityId = record.handledViaCashDrawerActivityId,
      cashDrawerActivityId = record.cashDrawerActivityId,
      paymentType = record.paymentType,
      assignedAt = record.assignedAt,
    )

}
