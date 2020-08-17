package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ SupplierLocationRecord, SupplierLocationUpdate }
import io.paytouch.core.entities.UserContext

trait SupplierLocationConversions {

  def toSupplierLocationUpdate(supplierId: UUID, locationId: UUID)(implicit user: UserContext): SupplierLocationUpdate =
    SupplierLocationUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(user.merchantId),
      supplierId = Some(supplierId),
      locationId = Some(locationId),
      active = None,
    )

  def toSupplierLocationUpdate(record: SupplierLocationRecord)(implicit user: UserContext): SupplierLocationUpdate =
    SupplierLocationUpdate(
      id = Some(record.id),
      merchantId = Some(user.merchantId),
      supplierId = Some(record.supplierId),
      locationId = Some(record.locationId),
      active = Some(record.active),
    )
}
