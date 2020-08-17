package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class ReturnOrderUpsertion(
    returnOrder: ReturnOrderUpdate,
    returnOrderProducts: Option[Seq[ReturnOrderProductUpdate]],
  ) extends UpsertionModel[ReturnOrderRecord]
