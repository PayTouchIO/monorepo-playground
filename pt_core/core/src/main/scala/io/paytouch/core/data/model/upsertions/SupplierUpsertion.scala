package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model._

final case class SupplierUpsertion(
    supplier: SupplierUpdate,
    supplierLocations: Map[UUID, Option[SupplierLocationUpdate]],
    supplierProducts: Option[Seq[SupplierProductUpdate]],
  ) extends UpsertionModel[SupplierRecord]
