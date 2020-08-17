package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class PurchaseOrderUpsertion(
    purchaseOrder: PurchaseOrderUpdate,
    purchaseOrderProducts: Option[Seq[PurchaseOrderProductUpdate]],
  ) extends UpsertionModel[PurchaseOrderRecord]
