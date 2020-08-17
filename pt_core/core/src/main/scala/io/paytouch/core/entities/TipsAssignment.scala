package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.entities.enums.{ ExposedName, HandledVia }

final case class TipsAssignment(
    id: UUID,
    locationId: UUID,
    userId: Option[UUID],
    orderId: Option[UUID],
    amount: MonetaryAmount,
    handledVia: HandledVia,
    handledViaCashDrawerActivityId: Option[UUID],
    cashDrawerActivityId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    assignedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.TipsAssignment
}

final case class TipsAssignmentUpsertion(
    id: UUID,
    locationId: UUID,
    userId: Option[UUID],
    orderId: Option[UUID],
    amount: BigDecimal,
    handledVia: HandledVia,
    handledViaCashDrawerActivityId: Option[UUID],
    cashDrawerActivityId: Option[UUID],
    paymentType: Option[TransactionPaymentType],
    assignedAt: ZonedDateTime,
  )
