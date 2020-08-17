package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.conversions.CategoryLocationConversions
import io.paytouch.core.data.model.{ CategoryLocationUpdate, CategoryUpdate, ImportRecord }
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait CategoryLocationExtractor extends Extractor with CategoryLocationConversions {

  def extractCategoryLocations(
      categories: Seq[CategoryUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[ErrorsOr[Seq[CategoryLocationUpdate]]] = {
    logExtraction("category locations")
    val categoryLocationUpdates = categories.flatMap(_.id).flatMap { categoryId =>
      importRecord.locationIds.map { locationId =>
        CategoryLocationUpdate(
          id = Some(UUID.randomUUID),
          merchantId = Some(importRecord.merchantId),
          categoryId = Some(categoryId),
          locationId = Some(locationId),
          active = None,
        )
      }
    }
    Future.successful(MultipleExtraction.success(categoryLocationUpdates))
  }
}
