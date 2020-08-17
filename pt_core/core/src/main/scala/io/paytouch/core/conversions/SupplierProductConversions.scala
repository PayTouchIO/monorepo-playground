package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.SupplierProductUpdate
import io.paytouch.core.entities.UserContext

trait SupplierProductConversions {

  def toSupplierProductUpdate(productId: UUID, supplierId: UUID)(implicit user: UserContext): SupplierProductUpdate =
    SupplierProductUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      supplierId = Some(supplierId),
    )
}
