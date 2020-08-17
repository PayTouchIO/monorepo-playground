package io.paytouch.core.async.importers.loaders

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import io.paytouch.core.async.importers.ProductImportData
import io.paytouch.core.async.trackers._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.entities.enums._
import io.paytouch.core.services.SetupStepService
import io.paytouch.core.withTag

class ProductImportLoader(
    val eventTracker: ActorRef withTag EventTracker,
    val setupStepService: SetupStepService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Loader[ProductImportData] {
  lazy val brandDao = daos.brandDao
  lazy val systemCategoryDao = daos.systemCategoryDao
  lazy val categoryLocationDao = daos.categoryLocationDao
  lazy val importDao = daos.importDao
  lazy val productDao = daos.productDao
  lazy val productLocationDao = daos.productLocationDao
  lazy val productLocationTaxRateDao = productDao.productLocationTaxRateDao
  lazy val productCategoryDao = daos.productCategoryDao
  lazy val productVariantOptionDao = daos.productVariantOptionDao
  lazy val stockDao = daos.stockDao
  lazy val supplierDao = daos.supplierDao
  lazy val supplierLocationDao = daos.supplierLocationDao
  lazy val supplierProductDao = daos.supplierProductDao
  lazy val taxRateLocationDao = daos.taxRateLocationDao
  lazy val variantOptionTypeDao = daos.variantOptionTypeDao
  lazy val variantOptionDao = daos.variantOptionDao
  lazy val variantProductDao = daos.variantProductDao

  def load(importer: ImportRecord, data: ProductImportData): Future[Unit] = {
    val importerId = importer.id
    for {
      _ <- deleteExistingProductsIfRequested(importerId)
      _ <- loadBrands(importerId, data.brands)
      _ <- loadCategories(importerId, data.categories)
      _ <- loadSubcategories(importerId, data.subcategories)
      _ <- loadCategoryLocations(importerId, data.categoryLocations)
      _ <- loadTemplateProducts(importerId, data.templateProducts)
      _ <- loadVariantProducts(importerId, data.variantProducts)
      _ <- loadSimpleProducts(importerId, data.simpleProducts)
      _ <- loadProductLocations(importerId, data.productLocations)
      _ <- loadProductCategories(importerId, data.productCategories)
      _ <- loadVariantOptionTypes(importerId, data.variantOptionTypes)
      _ <- loadVariantOptions(importerId, data.variantOptions)
      _ <- loadProductVariantOptions(importerId, data.productVariantOptions)
      _ <- loadStocks(importerId, data.stocks)
      _ <- loadSuppliers(importerId, data.suppliers)
      _ <- loadSupplierProducts(importerId, data.supplierProducts)
      _ <- loadSupplierLocations(importerId, data.supplierLocations)
      _ <- loadTaxRateLocations(importerId, data.taxRateLocations)
      _ <- loadProductLocationTaxRates(importerId, data.productLocationTaxRates)

      _ <- setupStepService.simpleCheckStepCompletion(importer.merchantId, MerchantSetupSteps.ImportProducts)
    } yield ()
  }

  def deleteExistingProductsIfRequested(importerId: UUID) =
    importDao.findById(importerId).flatMap {
      case Some(importer) if importer.deleteExisting =>
        Future.sequence {
          importer.locationIds.map { locationId =>
            logger.info(s"[Importer $importerId] deleting existing products associated to location $locationId")
            productDao.deleteByLocationId(locationId = locationId, merchantId = importer.merchantId).map {
              case (deletedProductLocations, deletedProducts) =>
                val deletedPLSize = deletedProductLocations.size
                val deletedPSize = deletedProducts.size
                logger.info(s"[Import $importerId] deleted $deletedPLSize product location and $deletedPSize products")
                deletedProducts.foreach(id => eventTracker ! DeletedItem(id, importer.merchantId, ExposedName.Product))
            }
          }
        }
      case _ => Future.successful(logger.info(s"[Import $importerId] no need to delete existing products"))
    }

  def loadCategories(importerId: UUID, categories: Seq[CategoryUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${categories.size} categories")
    systemCategoryDao.bulkUpsert(categories)
  }

  def loadBrands(importerId: UUID, brands: Seq[BrandUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${brands.size} brands")
    brandDao.bulkUpsert(brands)
  }

  def loadSubcategories(importerId: UUID, subcategories: Seq[CategoryUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${subcategories.size} subcategories")
    systemCategoryDao.bulkUpsert(subcategories)
  }

  def loadCategoryLocations(importerId: UUID, categoryLocations: Seq[CategoryLocationUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${categoryLocations.size} category locations")
    categoryLocationDao.bulkUpsertByRelIds(categoryLocations)
  }

  def loadTemplateProducts(importerId: UUID, templateProducts: Seq[ArticleUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${templateProducts.size} template products")
    productDao.bulkUpsert(templateProducts)
  }

  def loadVariantProducts(importerId: UUID, variantProducts: Seq[ArticleUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${variantProducts.size} variant products")
    variantProductDao
      .bulkUpsertAndDeleteTheRestByParentId(variantProducts, variantProducts.flatMap(_.isVariantOfProductId))
  }

  def loadSimpleProducts(importerId: UUID, simpleProducts: Seq[ArticleUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${simpleProducts.size} simple products")
    productDao.bulkUpsert(simpleProducts)
  }

  def loadProductLocations(importerId: UUID, productLocations: Seq[ProductLocationUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${productLocations.size} product locations")
    productLocationDao.bulkUpsertByRelIds(productLocations)
  }

  def loadProductCategories(importerId: UUID, productCategories: Seq[ProductCategoryUpsertion]) = {
    logger.info(s"[Importer $importerId] loading ${productCategories.size} product categories")
    productCategoryDao.bulkUpsertionByRelIds(productCategories)
  }

  def loadVariantOptionTypes(importerId: UUID, variantOptionTypes: Seq[VariantOptionTypeUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${variantOptionTypes.size} variant option types")
    variantOptionTypeDao.bulkUpsert(variantOptionTypes)
  }

  def loadVariantOptions(importerId: UUID, variantOptions: Seq[VariantOptionUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${variantOptions.size} variant options")
    variantOptionDao.bulkUpsertAndDeleteTheRestByProductIds(variantOptions, variantOptions.flatMap(_.productId))
  }

  def loadProductVariantOptions(importerId: UUID, productVariantOptions: Seq[ProductVariantOptionUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${productVariantOptions.size} product variant options")
    productVariantOptionDao
      .bulkUpsertAndDeleteTheRestByProductIds(productVariantOptions, productVariantOptions.flatMap(_.productId))
  }

  def loadStocks(importerId: UUID, stocks: Seq[StockUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${stocks.size} stocks")
    stockDao.bulkUpsert(stocks)
  }

  def loadSuppliers(importerId: UUID, suppliers: Seq[SupplierUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${suppliers.size} suppliers")
    supplierDao.bulkUpsert(suppliers)
  }

  def loadSupplierLocations(importerId: UUID, suppliersLocations: Seq[SupplierLocationUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${suppliersLocations.size} suppliers locations")
    supplierLocationDao.bulkUpsertByRelIds(suppliersLocations)
  }

  def loadSupplierProducts(importerId: UUID, suppliersProducts: Seq[SupplierProductUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${suppliersProducts.size} suppliers products")
    supplierProductDao.bulkUpsertByRelIds(suppliersProducts)
  }

  def loadTaxRateLocations(importerId: UUID, taxRateLocations: Seq[TaxRateLocationUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${taxRateLocations.size} tax rate locations")
    taxRateLocationDao.bulkUpsertByRelIds(taxRateLocations)
  }

  def loadProductLocationTaxRates(importerId: UUID, productLocationTaxRates: Seq[ProductLocationTaxRateUpdate]) = {
    logger.info(s"[Importer $importerId] loading ${productLocationTaxRates.size} product location tax rates")
    productLocationTaxRateDao.bulkUpsertByRelIds(productLocationTaxRates)
  }
}
