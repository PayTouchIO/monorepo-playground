package io.paytouch.core.async.importers.extractors

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.conversions.SupplierLocationConversions
import io.paytouch.core.data.model.{ ImportRecord, SupplierLocationUpdate, SupplierUpdate }
import io.paytouch.core.utils._

trait SupplierLocationExtractor extends Extractor with SupplierLocationConversions {
  def extractSupplierLocations(
      suppliers: Seq[SupplierUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[MultipleExtraction.ErrorsOr[Seq[SupplierLocationUpdate]]] = {
    logExtraction("supplier locations")
    val supplierLocationUpdates = suppliers.flatMap(_.id).flatMap { supplierId =>
      importRecord.locationIds.map { locationId =>
        SupplierLocationUpdate(
          id = Some(UUID.randomUUID),
          merchantId = Some(importRecord.merchantId),
          supplierId = Some(supplierId),
          locationId = Some(locationId),
          active = None,
        )
      }
    }
    Future.successful(MultipleExtraction.success(supplierLocationUpdates))
  }
}
