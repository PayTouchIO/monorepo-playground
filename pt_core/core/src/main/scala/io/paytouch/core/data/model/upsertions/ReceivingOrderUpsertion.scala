package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class ReceivingOrderUpsertion(
    receivingOrder: ReceivingOrderUpdate,
    receivingOrderProducts: Option[Seq[ReceivingOrderProductUpdate]],
  ) extends UpsertionModel[ReceivingOrderRecord]
