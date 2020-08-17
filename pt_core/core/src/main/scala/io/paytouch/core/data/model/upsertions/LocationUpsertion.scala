package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class LocationUpsertion(
    location: LocationUpdate,
    locationSettings: Option[LocationSettingsUpdate],
    locationEmailReceiptUpdate: LocationEmailReceiptUpdate,
    locationPrintReceiptUpdate: LocationPrintReceiptUpdate,
    locationReceiptUpdate: LocationReceiptUpdate,
    availabilities: Seq[AvailabilityUpdate],
    userLocations: Seq[UserLocationUpdate],
    initialOrderNumber: Option[Int],
  ) extends UpsertionModel[LocationRecord]
