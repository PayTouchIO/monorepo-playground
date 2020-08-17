package io.paytouch.core.entities

import java.util.UUID

final case class LocationTaxRates(locationId: UUID, taxRates: Seq[TaxRate])
