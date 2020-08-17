package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class InventoryCountUpsertion(
    inventoryCount: InventoryCountUpdate,
    inventoryCountProducts: Option[Seq[InventoryCountProductUpdate]],
  ) extends UpsertionModel[InventoryCountRecord]
