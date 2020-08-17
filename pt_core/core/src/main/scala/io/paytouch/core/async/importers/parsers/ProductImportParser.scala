package io.paytouch.core.async.importers.parsers

import java.io.File
import java.util.UUID

import com.github.tototoshi.csv.CSVReader
import io.paytouch.core.async.importers.ProductImportData
import io.paytouch.core.async.importers.extractors._
import io.paytouch.core.conversions.CategoryLocationConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._
import scala.util.{ Failure, Success, Try }

final case class ProductImportResult(
    simpleProductsToAdd: Int,
    simpleProductsToUpdate: Int,
    templateProductsToAdd: Int,
    templateProductsToUpdate: Int,
    variantProductsToAdd: Int,
    variantProductsToUpdate: Int,
    brandsToAdd: Int,
    brandsToUpdate: Int,
    categoriesToAdd: Int,
    categoriesToUpdate: Int,
    subcategoriesToAdd: Int,
    subcategoriesToUpdate: Int,
    variantOptionTypesToAdd: Int,
    variantOptionTypesToUpdate: Int,
    variantOptionsToAdd: Int,
    variantOptionsToUpdate: Int,
    stocksToAdd: Int,
    stocksToUpdate: Int,
    suppliersToAdd: Int,
    suppliersToUpdate: Int,
  )

case class EnrichedDataRow(
    rowNumber: Int,
    data: Map[String, List[String]],
    articleUpdates: Map[ArticleType, ArticleUpdateWithIdentifier] = Map.empty,
  ) {
  def getOrElse(key: String, default: Seq[String]): Seq[String] = data.getOrElse(key, default)
  val get = data.get _
  val contains = data.contains _
  val keys = data.keys
  val filterKeys = data.view.filterKeys _

  def withArticleUpdate(articleUpdate: ArticleUpdateWithIdentifier) = {
    require(articleUpdate.update.`type`.isDefined, "Article type must be defined")
    copy(articleUpdates = articleUpdates ++ Map(articleUpdate.update.`type`.get -> articleUpdate))
  }

  def articleUpdateByType(articleType: ArticleType): Option[ArticleUpdateWithIdentifier] =
    articleUpdates.get(articleType)

  lazy val storableArticleIds: Seq[UUID] = findFirstByTypes(ArticleType.storables).flatMap(_.update.id).toSeq
  lazy val mainArticleIds: Seq[UUID] = articleUpdates.values.flatMap(_.update.isVariantOfProductId).toSeq
  lazy val templateArticleId: Option[UUID] =
    articleUpdateByType(ArticleType.Variant).flatMap(_.update.isVariantOfProductId)

  def isForProductId(productId: Option[UUID]): Boolean =
    articleUpdates.values.exists(u => u.update.id == productId || u.update.isVariantOfProductId == productId)

  private def findFirstByTypes(articleTypes: Seq[ArticleType]): Option[ArticleUpdateWithIdentifier] =
    articleTypes.collectFirst {
      case articleType if articleUpdateByType(articleType).isDefined => articleUpdateByType(articleType).get
    }

  def +(other: EnrichedDataRow) = {
    require(rowNumber == other.rowNumber, "Can only sum instances referring to the same row number")
    copy(articleUpdates = articleUpdates ++ other.articleUpdates)
  }
}

class ProductImportParser(implicit val ec: ExecutionContext, val daos: Daos)
    extends Parser[ProductImportData]
       with Extractors
       with CategoryLocationConversions {

  type ImportResult = ProductImportResult

  private val locationDao = daos.locationDao

  def parse(importRecord: ImportRecord): Future[ErrorsOr[(ImportResult, ProductImportData)]] = {
    val parsedData = Try {
      val reader = CSVReader.open(new File(importRecord.filename))
      reader.allWithDuplicatedHeaders
    }
    parsedData match {
      case Failure(_)                                              => rejectCSVFile()
      case Success(data) if data.isEmpty || data.forall(_.isEmpty) => rejectCSVFile()
      case Success(data) =>
        for {
          locations <- locationDao.findByIds(importRecord.locationIds)
          enrichedDataRows = convertToCleanDataWithLineCount(data)
          productImportData <- extractProductImportData(enrichedDataRows)(importRecord, locations)
        } yield productImportData
    }
  }

  private def rejectCSVFile() = {
    val err = ValidationError(None, "Please provide a valid csv file with headers")
    Future.successful(MultipleExtraction.failure(err))
  }

  private def extractProductImportData(
      data: Seq[EnrichedDataRow],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ErrorsOr[(ImportResult, ProductImportData)]] =
    for {
      brands <- extractBrands(data)
      categories <- extractCategories(data)
      subcategories <- extractSubcategories(data, categories.entities)
      allCategoriesEntities = categories.entities ++ subcategories.entities
      categoryLocations <- extractCategoryLocations(allCategoriesEntities)
      (simpleProducts, simpleData) <- extractSimpleProducts(data, brands.entities)
      (templateProducts, templateData) <- extractTemplateProducts(data, brands.entities)
      (variantProducts, variantData) <- extractVariantProducts(data, templateProducts.entities, brands.entities)
      flatTemplateVariantsData = (variantData ++ templateData)
        .groupBy(_.rowNumber)
        .transform((_, v) => v.reduce(_ + _))
        .values
      data = (simpleData ++ flatTemplateVariantsData).sortBy(_.rowNumber)
      allProducts = templateProducts.entities ++ simpleProducts.entities ++ variantProducts.entities
      storableProducts = allProducts.filter(_.`type`.exists(_.isStorable))
      mainProducts = allProducts.filter(_.`type`.exists(_.isMain))
      productLocations <- extractProductLocations(data, allProducts)
      productCategories <- extractProductCategories(data, categories.entities, subcategories.entities, mainProducts)
      variantOptionTypes <- extractVariantOptionTypes(data)
      variantOpts <- extractVariantOptions(data, variantOptionTypes.entities)
      productVariantOpts <- extractProductVariantOptions(data, variantOpts.entities)
      stocks <- extractStocks(data)
      suppliers <- extractSuppliers(data)
      supplierLocations <- extractSupplierLocations(suppliers.entities)
      supplierProducts <- extractProductSuppliers(data, suppliers.entities, mainProducts)
      taxRateLocations <- extractTaxRateLocations(data)
      productLocationTaxRates <- extractProductLocationTaxRates(data, productLocations.entities, allProducts)
      upcs <- extractUpcs(data, storableProducts)
      (missingVariantProducts, missingProductVariantOpts) <- extractMissingVariantProductsWithOptions(
        variantOpts.entities,
        productVariantOpts.entities,
        variantProducts.entities,
      )
    } yield merge(
      brands,
      categories,
      subcategories,
      categoryLocations,
      simpleProducts,
      templateProducts,
      variantProducts,
      missingVariantProducts,
      productLocations,
      productCategories,
      variantOptionTypes,
      variantOpts,
      productVariantOpts,
      missingProductVariantOpts,
      stocks,
      suppliers,
      supplierLocations,
      supplierProducts,
      taxRateLocations,
      productLocationTaxRates,
      upcs,
    )

  private def convertToCleanDataWithLineCount(data: List[Map[String, List[String]]]): Seq[EnrichedDataRow] = {
    val cleanData = data.map(m => m.filterNot { case (_, v) => v.isEmpty } map { case (k, v) => (k.toLowerCase, v) })
    // Starting the line from 2 instead of 0: starting to count from 1 + skipping header line
    val initialLineCount = 2
    cleanData.zipWithIndex.flatMap {
      case (row, _) if row.values.forall(_.isEmpty) => None
      case (row, idx) =>
        Some(
          EnrichedDataRow(
            rowNumber = idx + initialLineCount,
            data = row,
          ),
        )
    }
  }

  def merge(
      brands: Extraction[BrandUpdate],
      categories: Extraction[CategoryUpdate],
      subcategories: Extraction[CategoryUpdate],
      categoryLocations: ErrorsOr[Seq[CategoryLocationUpdate]],
      simpleProducts: Extraction[ArticleUpdate],
      templateProducts: Extraction[ArticleUpdate],
      variantProducts: Extraction[ArticleUpdate],
      missingVariantProducts: ErrorsOr[Seq[ArticleUpdate]],
      productLocations: ErrorsOr[Seq[ProductLocationUpdate]],
      productCategories: ErrorsOr[Seq[ProductCategoryUpsertion]],
      variantOptionTypes: Extraction[VariantOptionTypeUpdate],
      variantOpts: Extraction[VariantOptionUpdate],
      productVariantOpts: ErrorsOr[Seq[ProductVariantOptionUpdate]],
      missingProductVariantOpts: ErrorsOr[Seq[ProductVariantOptionUpdate]],
      stocks: Extraction[StockUpdate],
      suppliers: Extraction[SupplierUpdate],
      supplierLocations: ErrorsOr[Seq[SupplierLocationUpdate]],
      supplierProducts: ErrorsOr[Seq[SupplierProductUpdate]],
      taxRateLocations: ErrorsOr[Seq[TaxRateLocationUpdate]],
      productLocationTaxRates: ErrorsOr[Seq[ProductLocationTaxRateUpdate]],
      upcs: ErrorsOr[Seq[String]],
    ): ErrorsOr[(ImportResult, ProductImportData)] =
    MultipleExtraction.combine(
      brands,
      categories,
      subcategories,
      categoryLocations,
      simpleProducts,
      templateProducts,
      variantProducts,
      missingVariantProducts,
      productLocations,
      productCategories,
      variantOptionTypes,
      variantOpts,
      productVariantOpts,
      missingProductVariantOpts,
      stocks,
      suppliers,
      supplierLocations,
      supplierProducts,
      taxRateLocations,
      productLocationTaxRates,
      upcs,
    ) {
      case (
            brnds,
            cats,
            subcats,
            catLogs,
            simplePrds,
            templatePrds,
            variantPrds,
            missingVariantPrds,
            prodLocs,
            prodCats,
            varOptTypes,
            varOpts,
            prodVarOpts,
            missingProdVarOpts,
            stcs,
            supplrs,
            supplrLocs,
            supplrPrds,
            txRtsLocs,
            prodLocTxRts,
            _,
          ) =>
        val importSummary = ProductImportResult(
          brandsToAdd = brnds.toAdd,
          brandsToUpdate = brnds.toUpdate,
          categoriesToAdd = cats.toAdd,
          categoriesToUpdate = cats.toUpdate,
          subcategoriesToAdd = subcats.toAdd,
          subcategoriesToUpdate = subcats.toUpdate,
          simpleProductsToAdd = simplePrds.toAdd,
          simpleProductsToUpdate = simplePrds.toUpdate,
          templateProductsToAdd = templatePrds.toAdd,
          templateProductsToUpdate = templatePrds.toUpdate,
          variantProductsToAdd = variantPrds.toAdd,
          variantProductsToUpdate = variantPrds.toUpdate,
          variantOptionTypesToAdd = varOptTypes.toAdd,
          variantOptionTypesToUpdate = varOptTypes.toUpdate,
          variantOptionsToAdd = varOpts.toAdd,
          variantOptionsToUpdate = varOpts.toUpdate,
          stocksToAdd = stcs.toAdd,
          stocksToUpdate = stcs.toUpdate,
          suppliersToAdd = supplrs.toAdd,
          suppliersToUpdate = supplrs.toUpdate,
        )

        val productImportData = ProductImportData(
          brands = brnds.updates,
          categories = cats.updates,
          subcategories = subcats.updates,
          categoryLocations = catLogs,
          simpleProducts = simplePrds.updates,
          templateProducts = templatePrds.updates,
          variantProducts = variantPrds.updates ++ missingVariantPrds,
          productLocations = prodLocs,
          productCategories = prodCats,
          variantOptionTypes = varOptTypes.updates,
          variantOptions = varOpts.updates,
          productVariantOptions = prodVarOpts ++ missingProdVarOpts,
          stocks = stcs.updates,
          suppliers = supplrs.updates,
          supplierLocations = supplrLocs,
          supplierProducts = supplrPrds,
          taxRateLocations = txRtsLocs,
          productLocationTaxRates = prodLocTxRts,
        )

        (importSummary, productImportData)
    }
}

trait Extractors
    extends BrandExtractor
       with CategoryExtractor
       with CategoryLocationExtractor
       with MissingVariantProductExtractor
       with ProductCategoryExtractor
       with ProductExtractor
       with ProductLocationExtractor
       with ProductLocationTaxRateExtractor
       with ProductVariantOptionExtractor
       with SubcategoryExtractor
       with SimpleProductExtractor
       with StockExtractor
       with SupplierExtractor
       with SupplierProductExtractor
       with SupplierLocationExtractor
       with TaxRateLocationExtractor
       with TemplateProductExtractor
       with UpcExtractor
       with VariantProductExtractor
       with VariantOptionExtractor
       with VariantOptionTypeExtractor
