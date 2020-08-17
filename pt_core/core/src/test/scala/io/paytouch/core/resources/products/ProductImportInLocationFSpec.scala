package io.paytouch.core.resources.products

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.RouteTestTimeout
import io.paytouch.core.async.importers.parsers.{ ProductImportResult, ValidationResult }
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.data.model.enums.{ ArticleType, ImportStatus }
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

import scala.concurrent.duration._

class ProductImportInLocationFSpec extends FSpec {
  override implicit val timeout = RouteTestTimeout(4 minutes)

  abstract class ProductImportResourceFSpecContext extends FSpecContext with Fixtures with SetupStepsAssertions {

    val importDao = daos.importDao
    val brandDao = daos.brandDao
    val categoryDao = daos.categoryDao
    val categoryLocationDao = daos.categoryLocationDao
    val productDao = daos.productDao
    val productLocationDao = daos.productLocationDao
    val productLocationTaxRateDao = daos.productLocationTaxRateDao
    val productCategoryDao = daos.productCategoryDao
    val productVariantOptionDao = daos.productVariantOptionDao
    val stockDao = daos.stockDao
    val supplierDao = daos.supplierDao
    val supplierLocationDao = daos.supplierLocationDao
    val supplierProductDao = daos.supplierProductDao
    val taxRateLocationDao = daos.taxRateLocationDao
    val variantOptionTypeDao = daos.variantOptionTypeDao
    val variantOptionDao = daos.variantOptionDao

    def assertProductRecordRespectsInvariants(record: ArticleRecord) = {
      if (record.`type` == ArticleType.Simple) {
        record.isVariantOfProductId ==== Some(record.id)
        record.hasVariant ==== false
      }

      if (record.`type` == ArticleType.Template) {
        record.isVariantOfProductId !=== Some(record.id)
        record.upc ==== None
        record.sku ==== None
        record.hasVariant ==== true
      }
    }

    defaultMenuCatalog // trigger initialization of lazy val
  }

  "POST /v1/products.import (dryRun = true)" in {
    "if request has valid token" in {

      "if the product import and location belongs to the merchant" should {
        "analyze a valid csv file (deleteExisting = true)" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=true&delete_existing=true",
            FileSupport.ValidCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Successful
              importRecord.validationErrors ==== None
              val productImportResult = importRecord.importSummary.get.extract[ProductImportResult]
              productImportResult.brandsToAdd ==== 4
              productImportResult.brandsToUpdate ==== 0
              productImportResult.categoriesToAdd ==== 2
              productImportResult.categoriesToUpdate ==== 0
              productImportResult.subcategoriesToAdd ==== 3
              productImportResult.subcategoriesToUpdate ==== 0
              productImportResult.simpleProductsToAdd ==== 1
              productImportResult.simpleProductsToUpdate ==== 0
              productImportResult.templateProductsToAdd ==== 2
              productImportResult.templateProductsToUpdate ==== 0
              productImportResult.variantProductsToAdd ==== 4
              productImportResult.variantProductsToUpdate ==== 0
              productImportResult.variantOptionTypesToAdd ==== 2
              productImportResult.variantOptionTypesToUpdate ==== 0
              productImportResult.variantOptionsToAdd ==== 4
              productImportResult.variantOptionsToUpdate ==== 0
              productImportResult.stocksToAdd ==== 4
              productImportResult.stocksToUpdate ==== 0
              productImportResult.suppliersToAdd ==== 1
              productImportResult.suppliersToUpdate ==== 0
            }

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.importStatus ==== ImportStatus.NotStarted
              brandDao.findAllByMerchantId(merchant.id).await.size ==== 0
              categoryDao.findAllByMerchantId(merchant.id).await.size ==== 0
              productDao.findAllByMerchantId(merchant.id).await.size ==== 0
              variantOptionTypeDao.findAllByMerchantId(merchant.id).await.size ==== 0
              variantOptionDao.findAllByMerchantId(merchant.id).await.size ==== 0
              stockDao.findAllByMerchantId(merchant.id).await.size ==== 0
              supplierDao.findAllByMerchantId(merchant.id).await.size ==== 0
            }
          }
        }

        "analyze a valid csv file (deleteExisting = false)" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=true&delete_existing=false",
            FileSupport.ValidCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Successful
              importRecord.importStatus ==== ImportStatus.NotStarted
              importRecord.validationErrors ==== None
              val productImportResult = importRecord.importSummary.get.extract[ProductImportResult]
              productImportResult.brandsToAdd ==== 4
              productImportResult.brandsToUpdate ==== 0
              productImportResult.categoriesToAdd ==== 2
              productImportResult.categoriesToUpdate ==== 0
              productImportResult.subcategoriesToAdd ==== 3
              productImportResult.subcategoriesToUpdate ==== 0
              productImportResult.simpleProductsToAdd ==== 1
              productImportResult.simpleProductsToUpdate ==== 0
              productImportResult.templateProductsToAdd ==== 2
              productImportResult.templateProductsToUpdate ==== 0
              productImportResult.variantProductsToAdd ==== 4
              productImportResult.variantProductsToUpdate ==== 0
              productImportResult.variantOptionTypesToAdd ==== 2
              productImportResult.variantOptionTypesToUpdate ==== 0
              productImportResult.variantOptionsToAdd ==== 4
              productImportResult.variantOptionsToUpdate ==== 0
              productImportResult.stocksToAdd ==== 4
              productImportResult.stocksToUpdate ==== 0
              productImportResult.suppliersToAdd ==== 1
              productImportResult.suppliersToUpdate ==== 0
            }

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.importStatus ==== ImportStatus.NotStarted
              brandDao.findAllByMerchantId(merchant.id).await.size ==== 0
              categoryDao.findAllByMerchantId(merchant.id).await.size ==== 0
              productDao.findAllByMerchantId(merchant.id).await.size ==== 0
              variantOptionTypeDao.findAllByMerchantId(merchant.id).await.size ==== 0
              variantOptionDao.findAllByMerchantId(merchant.id).await.size ==== 0
              stockDao.findAllByMerchantId(merchant.id).await.size ==== 0
              supplierDao.findAllByMerchantId(merchant.id).await.size ==== 0
            }
          }
        }

        "reject a not well formed csv file" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=true&delete_existing=true",
            FileSupport.BadCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Failed
              importRecord.importStatus ==== ImportStatus.NotStarted
              importRecord.importSummary ==== None
              importRecord.validationErrors.get.extract[ValidationResult] ==== ValidationResult(
                "Please provide a valid csv file with headers",
              )
            }
          }
        }

        "reject a broken csv file" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=true&delete_existing=true",
            FileSupport.BrokenCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Failed
              importRecord.importStatus ==== ImportStatus.NotStarted
              importRecord.importSummary ==== None
              importRecord.validationErrors.get.extract[ValidationResult] ==== ValidationResult(
                "Please provide a valid csv file with headers",
              )
            }
          }
        }
      }

      "if the product import does not belong to the merchant" should {
        "return 404" in new ProductImportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorImport = Factory.`import`(competitorLocation).create

          MultiformDataRequest(
            s"/v1/products.import?import_id=${competitorImport.id}&location_id[]=${rome.id}&dry_run=true&delete_existing=true",
            FileSupport.GenericCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the location is not accessible to current user" should {
        "return 404" in new ProductImportResourceFSpecContext {
          val newYork = Factory.location(merchant).create

          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${newYork.id}&dry_run=true&delete_existing=true",
            FileSupport.GenericCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the location does not belong to the merchant" should {
        "return 404" in new ProductImportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${competitorLocation.id}&dry_run=true&delete_existing=true",
            FileSupport.GenericCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  "POST /v1/products.import (dryRun = false)" in {
    "if request has valid token" in {

      "if the product import and location belongs to the merchant" should {
        "import a valid csv file (deleteExisting = true)" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=false&delete_existing=true",
            FileSupport.ValidCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Successful
              importRecord.importStatus ==== ImportStatus.Successful
              importRecord.validationErrors ==== None
              val productImportResult = importRecord.importSummary.get.extract[ProductImportResult]
              productImportResult.brandsToAdd ==== 4
              productImportResult.brandsToUpdate ==== 0
              productImportResult.categoriesToAdd ==== 2
              productImportResult.categoriesToUpdate ==== 0
              productImportResult.subcategoriesToAdd ==== 3
              productImportResult.subcategoriesToUpdate ==== 0
              productImportResult.simpleProductsToAdd ==== 1
              productImportResult.simpleProductsToUpdate ==== 0
              productImportResult.templateProductsToAdd ==== 2
              productImportResult.templateProductsToUpdate ==== 0
              productImportResult.variantProductsToAdd ==== 4
              productImportResult.variantProductsToUpdate ==== 0
              productImportResult.variantOptionTypesToAdd ==== 2
              productImportResult.variantOptionTypesToUpdate ==== 0
              productImportResult.variantOptionsToAdd ==== 4
              productImportResult.variantOptionsToUpdate ==== 0
              productImportResult.stocksToAdd ==== 4
              productImportResult.stocksToUpdate ==== 0
              productImportResult.suppliersToAdd ==== 1
              productImportResult.suppliersToUpdate ==== 0
            }

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Successful

              val brands = brandDao.findAllByMerchantId(merchant.id).await
              brands.size ==== 4

              val categories = categoryDao.findAllByMerchantId(merchant.id).await
              categories.size ==== 5

              val categoryLocations = categoryLocationDao.findByItemIds(categories.map(_.id), Some(rome.id)).await
              categoryLocations.size ==== 5

              val products = productDao.findAllByMerchantId(merchant.id).await
              products.size ==== 7
              products.map(assertProductRecordRespectsInvariants)

              val productIds = products.map(_.id)

              val productLocations = productLocationDao.findByProductIdsAndLocationId(productIds, rome.id).await
              productLocations.size ==== 7

              val productCategories = productCategoryDao.findByProductIds(productIds).await
              productCategories.size ==== 5

              val variantOptionTypes = variantOptionTypeDao.findByProductIds(productIds).await
              variantOptionTypes.size ==== 2

              val variantOptions = variantOptionDao.findByVariantOptionTypeIds(variantOptionTypes.map(_.id)).await
              variantOptions.size ==== 4

              val productVariantOptions = productVariantOptionDao.findByProductIds(products.map(_.id)).await
              productVariantOptions.size ==== 4

              val stocks = stockDao.findByProductIdsAndMerchantId(productIds, merchant.id).await
              stocks.size ==== 4

              val suppliers = supplierDao.findAllByMerchantId(merchant.id).await
              suppliers.size ==== 1

              val supplierLocations =
                supplierLocationDao.findByItemIdsAndLocationIds(suppliers.map(_.id), Seq(rome.id)).await
              supplierLocations.size ==== 1

              val supplierProducts =
                supplierProductDao.findBySupplierIdsAndProductIds(suppliers.map(_.id), productIds).await
              supplierProducts.size ==== 1

              val taxRateLocations = taxRateLocationDao.findByItemId(taxRate.id).await
              taxRateLocations.size ==== 1

              val productLocationTaxRates = productLocationTaxRateDao.findByTaxRateIds(Seq(taxRate.id)).await
              productLocationTaxRates.size ==== 2

            }

            assertSetupStepCompleted(merchant, MerchantSetupSteps.ImportProducts)
          }
        }

        "import a valid csv file (deleteExisting = false)" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=false&delete_existing=false",
            FileSupport.ValidCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Successful
              importRecord.validationErrors ==== None
              val productImportResult = importRecord.importSummary.get.extract[ProductImportResult]
              productImportResult.brandsToAdd ==== 4
              productImportResult.brandsToUpdate ==== 0
              productImportResult.categoriesToAdd ==== 2
              productImportResult.categoriesToUpdate ==== 0
              productImportResult.subcategoriesToAdd ==== 3
              productImportResult.subcategoriesToUpdate ==== 0
              productImportResult.simpleProductsToAdd ==== 1
              productImportResult.simpleProductsToUpdate ==== 0
              productImportResult.templateProductsToAdd ==== 2
              productImportResult.templateProductsToUpdate ==== 0
              productImportResult.variantProductsToAdd ==== 4
              productImportResult.variantProductsToUpdate ==== 0
              productImportResult.variantOptionTypesToAdd ==== 2
              productImportResult.variantOptionTypesToUpdate ==== 0
              productImportResult.variantOptionsToAdd ==== 4
              productImportResult.variantOptionsToUpdate ==== 0
              productImportResult.stocksToAdd ==== 4
              productImportResult.stocksToUpdate ==== 0
              productImportResult.suppliersToAdd ==== 1
              productImportResult.suppliersToUpdate ==== 0
            }

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.importStatus ==== ImportStatus.Successful

              val brands = brandDao.findAllByMerchantId(merchant.id).await
              brands.size ==== 4

              val categories = categoryDao.findAllByMerchantId(merchant.id).await
              categories.size ==== 5

              val categoryLocations = categoryLocationDao.findByItemIds(categories.map(_.id), Some(rome.id)).await
              categoryLocations.size ==== 5

              val products = productDao.findAllByMerchantId(merchant.id).await
              products.size ==== 7
              products.map(assertProductRecordRespectsInvariants)

              val productIds = products.map(_.id)

              val productLocations = productLocationDao.findByProductIdsAndLocationId(productIds, rome.id).await
              productLocations.size ==== 7

              val productCategories = productCategoryDao.findByProductIds(productIds).await
              productCategories.size ==== 5

              val variantOptionTypes = variantOptionTypeDao.findByProductIds(productIds).await
              variantOptionTypes.size ==== 2

              val variantOptions = variantOptionDao.findByVariantOptionTypeIds(variantOptionTypes.map(_.id)).await
              variantOptions.size ==== 4

              val productVariantOptions = variantOptionDao.findByProductIds(products.map(_.id)).await
              productVariantOptions.size ==== 4

              val stocks = stockDao.findByProductIdsAndMerchantId(productIds, merchant.id).await
              stocks.size ==== 4

              val suppliers = supplierDao.findAllByMerchantId(merchant.id).await
              suppliers.size ==== 1

              val supplierLocations =
                supplierLocationDao.findByItemIdsAndLocationIds(suppliers.map(_.id), Seq(rome.id)).await
              supplierLocations.size ==== 1

              val supplierProducts =
                supplierProductDao.findBySupplierIdsAndProductIds(suppliers.map(_.id), productIds).await
              supplierProducts.size ==== 1

              val taxRateLocations = taxRateLocationDao.findByItemId(taxRate.id).await
              taxRateLocations.size ==== 1

              val productLocationTaxRates = productLocationTaxRateDao.findByTaxRateIds(Seq(taxRate.id)).await
              productLocationTaxRates.size ==== 2

            }

            assertSetupStepCompleted(merchant, MerchantSetupSteps.ImportProducts)
          }
        }

        "import a valid csv file with only variant products" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=false&delete_existing=false",
            FileSupport.ValidCsvFileOnlyVariants,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Successful
              importRecord.validationErrors ==== None
              val productImportResult = importRecord.importSummary.get.extract[ProductImportResult]
              productImportResult.brandsToAdd ==== 0
              productImportResult.brandsToUpdate ==== 0
              productImportResult.categoriesToAdd ==== 1
              productImportResult.categoriesToUpdate ==== 0
              productImportResult.subcategoriesToAdd ==== 1
              productImportResult.subcategoriesToUpdate ==== 0
              productImportResult.simpleProductsToAdd ==== 0
              productImportResult.simpleProductsToUpdate ==== 0
              productImportResult.templateProductsToAdd ==== 1
              productImportResult.templateProductsToUpdate ==== 0
              productImportResult.variantProductsToAdd ==== 5
              productImportResult.variantProductsToUpdate ==== 0
              productImportResult.variantOptionTypesToAdd ==== 3
              productImportResult.variantOptionTypesToUpdate ==== 0
              productImportResult.variantOptionsToAdd ==== 6
              productImportResult.variantOptionsToUpdate ==== 0
              productImportResult.stocksToAdd ==== 5
              productImportResult.stocksToUpdate ==== 0
              productImportResult.suppliersToAdd ==== 0
              productImportResult.suppliersToUpdate ==== 0
            }

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.importStatus ==== ImportStatus.Successful

              val brands = brandDao.findAllByMerchantId(merchant.id).await
              brands.size ==== 0

              val categories = categoryDao.findAllByMerchantId(merchant.id).await
              categories.size ==== 2

              val categoryLocations = categoryLocationDao.findByItemIds(categories.map(_.id), Some(rome.id)).await
              categoryLocations.size ==== 2

              val products = productDao.findAllByMerchantId(merchant.id).await
              products.size ==== 7

              val productIds = products.map(_.id)

              val productLocations = productLocationDao.findByProductIdsAndLocationId(productIds, rome.id).await
              productLocations.size ==== 6

              val productCategories = productCategoryDao.findByProductIds(productIds).await
              productCategories.size ==== 1

              val variantOptionTypes = variantOptionTypeDao.findByProductIds(productIds).await
              variantOptionTypes.size ==== 3

              val variantOptions = variantOptionDao.findByVariantOptionTypeIds(variantOptionTypes.map(_.id)).await
              variantOptions.size ==== 6

              val productVariantOptions = variantOptionDao.findByProductIds(products.map(_.id)).await
              productVariantOptions.size ==== 6

              val stocks = stockDao.findByProductIdsAndMerchantId(productIds, merchant.id).await
              stocks.size ==== 5

              val suppliers = supplierDao.findAllByMerchantId(merchant.id).await
              suppliers.size ==== 0

              val supplierLocations =
                supplierLocationDao.findByItemIdsAndLocationIds(suppliers.map(_.id), Seq(rome.id)).await
              supplierLocations.size ==== 0

              val supplierProducts =
                supplierProductDao.findBySupplierIdsAndProductIds(suppliers.map(_.id), productIds).await
              supplierProducts.size ==== 0

              val taxRateLocations = taxRateLocationDao.findByItemId(taxRate.id).await
              taxRateLocations.size ==== 1

              val productLocationTaxRates = productLocationTaxRateDao.findByTaxRateIds(Seq(taxRate.id)).await
              productLocationTaxRates.size ==== 1

            }

            assertSetupStepCompleted(merchant, MerchantSetupSteps.ImportProducts)
          }
        }

        "reject a not well formed csv file" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=false&delete_existing=true",
            FileSupport.BadCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Failed
              importRecord.importStatus ==== ImportStatus.NotStarted
              importRecord.importSummary ==== None
              importRecord.validationErrors.get.extract[ValidationResult] ==== ValidationResult(
                "Please provide a valid csv file with headers",
              )
            }
          }
        }

        "reject a broken csv file" in new ProductImportResourceFSpecContext {
          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${rome.id}&dry_run=false&delete_existing=true",
            FileSupport.BrokenCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            afterAWhile {
              val importRecord = importDao.findById(importRecordId).await.get
              importRecord.validationStatus ==== ImportStatus.Failed
              importRecord.importStatus ==== ImportStatus.NotStarted
              importRecord.importSummary ==== None
              importRecord.validationErrors.get.extract[ValidationResult] ==== ValidationResult(
                "Please provide a valid csv file with headers",
              )
            }
          }
        }
      }

      "if the product import does not belong to the merchant" should {
        "return 404" in new ProductImportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorImport = Factory.`import`(competitorLocation).create

          MultiformDataRequest(
            s"/v1/products.import?import_id=${competitorImport.id}&location_id[]=${rome.id}&dry_run=false&delete_existing=true",
            FileSupport.GenericCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the location is not accessible to current user" should {
        "return 404" in new ProductImportResourceFSpecContext {
          val newYork = Factory.location(merchant).create

          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${newYork.id}&dry_run=false&delete_existing=true",
            FileSupport.GenericCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the location does not belong to the merchant" should {
        "return 404" in new ProductImportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          MultiformDataRequest(
            s"/v1/products.import?import_id=$importRecordId&location_id[]=${competitorLocation.id}&dry_run=false&delete_existing=true",
            FileSupport.GenericCsvFile,
            "csv",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  trait Fixtures extends MultipleLocationFixtures {
    val importRecordId = UUID.randomUUID

    val taxRate = Factory.taxRate(merchant, name = Some("City Tax"), locations = Seq.empty).create
  }

  object FileSupport {

    lazy val ValidCsvFile = loadFile("imports/valid-product-import.csv")
    lazy val ValidCsvFileOnlyVariants = loadFile("imports/valid-product-import-only-variants.csv")
    lazy val GenericCsvFile = writeToFile(List("a csv file"))
    lazy val BadCsvFile = writeToFile(List("This is not a well formed csv file!"))
    lazy val BrokenCsvFile = loadFile("imports/broken-import.csv")
  }
}
