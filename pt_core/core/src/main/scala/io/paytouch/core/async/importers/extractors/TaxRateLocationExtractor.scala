package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait TaxRateLocationExtractor extends Extractor {

  private val taxRateDao = daos.taxRateDao

  def extractTaxRateLocations(
      data: Seq[EnrichedDataRow],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ErrorsOr[Seq[TaxRateLocationUpdate]]] = {
    logExtraction("tax rate locations")
    val cleanData = data.filter(_.contains(Keys.TaxRateName))
    val taxNames: Seq[String] = cleanData.flatMap(_.get(Keys.TaxRateName)).flatten
    for {
      taxRates <- taxRateDao.findByNamesAndMerchantId(taxNames, importRecord.merchantId)
    } yield buildTaxRateLocationUpdates(taxRates, locations)
  }

  private def buildTaxRateLocationUpdates(
      taxRates: Seq[TaxRateRecord],
      locations: Seq[LocationRecord],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[TaxRateLocationUpdate]] =
    MultipleExtraction.success {
      for {
        location <- locations
        taxRate <- taxRates
      } yield toTaxRateLocationUpdate(taxRate, location)
    }

  private def toTaxRateLocationUpdate(
      taxRate: TaxRateRecord,
      location: LocationRecord,
    )(implicit
      importRecord: ImportRecord,
    ): TaxRateLocationUpdate =
    TaxRateLocationUpdate(
      id = None,
      merchantId = Some(importRecord.merchantId),
      taxRateId = Some(taxRate.id),
      locationId = Some(location.id),
      active = Some(true),
    )
}
