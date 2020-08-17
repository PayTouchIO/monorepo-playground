package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model.{ TaxRateLocationUpdate, TaxRateRecord, TaxRateUpdate }

final case class TaxRateUpsertion(taxRate: TaxRateUpdate, taxRateLocations: Map[UUID, Option[TaxRateLocationUpdate]])
    extends UpsertionModel[TaxRateRecord]
