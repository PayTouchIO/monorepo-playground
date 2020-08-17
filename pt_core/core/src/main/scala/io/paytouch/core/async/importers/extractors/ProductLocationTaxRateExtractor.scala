package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait ProductLocationTaxRateExtractor extends Extractor {

  private val taxRateDao = daos.taxRateDao

  def extractProductLocationTaxRates(
      data: Seq[EnrichedDataRow],
      productLocations: Seq[ProductLocationUpdate],
      products: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[ErrorsOr[Seq[ProductLocationTaxRateUpdate]]] = {
    logExtraction("product location tax rates")
    val nonVariantProducts = products.filterNot(_.`type`.exists(_.isVariant))
    val cleanData = data.filter(_.contains(Keys.TaxRateName))
    val taxNames: Seq[String] = cleanData.flatMap(row => row.get(Keys.TaxRateName)).flatten
    for {
      taxRates <- taxRateDao.findByNamesAndMerchantId(taxNames, importRecord.merchantId)
    } yield MultipleExtraction.sequence {
      cleanData.map(row => extractProductLocationTaxRatePerRow(row, productLocations, taxRates, nonVariantProducts))
    }
  }

  private def extractProductLocationTaxRatePerRow(
      row: EnrichedDataRow,
      productLocations: Seq[ProductLocationUpdate],
      taxRates: Seq[TaxRateRecord],
      nonVariantProducts: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[ProductLocationTaxRateUpdate]] = {
    val productLocationTaxRates = for {
      matchingProductLocation <- findProductLocationsPerRow(row, productLocations, nonVariantProducts)
      taxRateNames = row.getOrElse(Keys.TaxRateName, Seq.empty)
      matchingTaxRate <- findMatchingTaxRates(matchingProductLocation, taxRateNames, taxRates)
    } yield toProductLocationTaxRate(matchingProductLocation, matchingTaxRate)
    MultipleExtraction.success(productLocationTaxRates)
  }

  private def findMatchingTaxRates(
      productLocation: ProductLocationUpdate,
      taxRateNames: Seq[String],
      taxRates: Seq[TaxRateRecord],
    ): Seq[TaxRateRecord] = {
    val namesLowerCase = taxRateNames.map(_.toLowerCase)
    taxRates.filter(tr => namesLowerCase.contains(tr.name.toLowerCase))
  }

  private def toProductLocationTaxRate(
      productLocation: ProductLocationUpdate,
      taxRate: TaxRateRecord,
    )(implicit
      importRecord: ImportRecord,
    ): ProductLocationTaxRateUpdate =
    ProductLocationTaxRateUpdate(
      id = None,
      merchantId = Some(importRecord.merchantId),
      taxRateId = Some(taxRate.id),
      productLocationId = productLocation.id,
    )
}
