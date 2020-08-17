package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class CashDrawerUpsertion(
    cashDrawerUpdate: CashDrawerUpdate,
    cashDrawerActivities: Seq[CashDrawerActivityUpdate],
  ) extends UpsertionModel[CashDrawerRecord]
