package io.paytouch.core.async.importers

import akka.actor.ActorRef
import awscala.s3.Bucket
import io.paytouch.core.S3ImportsBucket
import io.paytouch.core.async.importers.loaders.ProductImportLoader
import io.paytouch.core.async.importers.parsers.ProductImportParser
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.services.SetupStepService
import io.paytouch.utils.Tagging.withTag

import scala.concurrent.ExecutionContext

final case class ProductImportData(
    brands: Seq[BrandUpdate],
    categories: Seq[CategoryUpdate],
    subcategories: Seq[CategoryUpdate],
    categoryLocations: Seq[CategoryLocationUpdate],
    simpleProducts: Seq[ArticleUpdate],
    templateProducts: Seq[ArticleUpdate],
    variantProducts: Seq[ArticleUpdate],
    productLocations: Seq[ProductLocationUpdate],
    productCategories: Seq[ProductCategoryUpsertion],
    variantOptionTypes: Seq[VariantOptionTypeUpdate],
    variantOptions: Seq[VariantOptionUpdate],
    productVariantOptions: Seq[ProductVariantOptionUpdate],
    stocks: Seq[StockUpdate],
    suppliers: Seq[SupplierUpdate],
    supplierLocations: Seq[SupplierLocationUpdate],
    supplierProducts: Seq[SupplierProductUpdate],
    taxRateLocations: Seq[TaxRateLocationUpdate],
    productLocationTaxRates: Seq[ProductLocationTaxRateUpdate],
  )

class ProductImporter(
    val eventTracker: ActorRef withTag EventTracker,
    val setupStepService: SetupStepService,
    val s3Client: S3Client,
    val uploadBucket: Bucket withTag S3ImportsBucket,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Importer {

  type Data = ProductImportData
  val parser = new ProductImportParser
  val loader = new ProductImportLoader(eventTracker, setupStepService)
}
