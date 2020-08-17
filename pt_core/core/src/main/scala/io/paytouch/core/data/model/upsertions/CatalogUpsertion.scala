package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class CatalogUpsertion(catalog: CatalogUpdate, availabilities: Seq[AvailabilityUpdate])
    extends UpsertionModel[CatalogRecord]
