package io.paytouch.core.async.importers.extractors

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.async.importers.parsers.{ EnrichedDataRow, ValidationError }
import io.paytouch.core.async.importers.{ Keys, Rows, UpdatesWithCount }
import io.paytouch.core.calculations.MarginCalculation
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, UnitType }
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction._
import io.paytouch.core.validators.ArticleValidator

import scala.concurrent._
import scala.util.Try

case class ArticleUpdateWithIdentifier(
    update: ArticleUpdate,
    identifier: String,
    hasExistingRecord: Boolean = false,
  ) {
  def withExistingRecordIds(record: ArticleIdentifierRecord) =
    copy(
      update = update.copy(id = Some(record.id), isVariantOfProductId = record.isVariantOfProductId),
      hasExistingRecord = true,
    )

  def withNewIds = {
    val id = Some(UUID.randomUUID)
    val isVariantOfProductId =
      if (update.`type`.exists(_.isSimple)) id else update.isVariantOfProductId
    copy(update = update.copy(id = id, isVariantOfProductId = isVariantOfProductId))
  }
}

trait ProductExtractor extends Extractor with MarginCalculation {

  val articleIdentifierDao = daos.articleIdentifierDao
  val productDao = daos.productDao
  val articleValidator = new ArticleValidator

  protected def extractProducts(
      data: Rows,
      productType: ArticleType,
      isVariantOfProductId: Option[UUID],
      brands: Seq[BrandUpdate],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ExtractionWithData[ArticleUpdate]] = {
    val extractedProducts = MultipleExtraction.sequence(data.map { row =>
      extractGenericProductPerRow(row, productType, isVariantOfProductId, brands).map(Seq(_))
    })
    val extractedProductsWithDefault =
      extractedProducts.getOrElse(Seq.empty).flatMap(_.articleUpdateByType(productType))
    val names = extractedProductsWithDefault.flatMap(_.update.name).distinct
    val upcs = extractedProductsWithDefault.flatMap(_.update.upc.toOption).distinct
    val skus = extractedProductsWithDefault.flatMap(_.update.sku.toOption).distinct
    val variantOptionIdentifiers = extractedProductsWithDefault.map(_.identifier).distinct

    articleIdentifierDao
      .findPotentialMatch(
        importRecord.merchantId,
        productType,
        names = names,
        upcs = upcs,
        skus = skus,
        variantOptionIdentifiers = variantOptionIdentifiers,
      )
      .map { existingProducts =>
        val extractedWithUpdatedIds =
          updateIdsToExistingProducts(productType, existingProducts, extractedProducts, importRecord.deleteExisting)
        (extractedWithUpdatedIds.map(toUpdatesWithCount(_, productType)), extractedWithUpdatedIds.getOrElse(data))
      }
  }

  private def updateIdsToExistingProducts(
      productType: ArticleType,
      existingProducts: Seq[ArticleIdentifierRecord],
      extractedProducts: ErrorsOr[Seq[EnrichedDataRow]],
      deleteExisting: Boolean,
    ): ErrorsOr[Seq[EnrichedDataRow]] =
    extractedProducts.map {
      _.flatMap { row =>
        row.articleUpdateByType(productType).map { updateWithIdentifier =>
          val newUpdateWithIdentifier = updateWithIdentifier match {
            case _ if !deleteExisting =>
              findAMatchableProduct(existingProducts, updateWithIdentifier)
                .map(updateWithIdentifier.withExistingRecordIds)
            case _ => None
          }
          row.withArticleUpdate(newUpdateWithIdentifier.getOrElse(updateWithIdentifier.withNewIds))
        }
      }
    }

  private def findAMatchableProduct(
      existingProducts: Seq[ArticleIdentifierRecord],
      updateWithIdentifier: ArticleUpdateWithIdentifier,
    ): Option[ArticleIdentifierRecord] = {
    val update = updateWithIdentifier.update
    val matchableProducts = {
      val sameTypes =
        existingProducts.filter(p => update.`type`.contains(p.`type`))
      if (update.upc.isDefined) sameTypes.filter(p => update.upc.toOption == p.upc)
      else if (update.sku.isDefined)
        sameTypes.filter(p => update.sku.toOption == p.sku && p.variantOptions == updateWithIdentifier.identifier)
      else sameTypes.filter(p => update.name.contains(p.name) && p.variantOptions == updateWithIdentifier.identifier)
    }
    if (matchableProducts.size > 1) {
      val matchedIds = matchableProducts.map(_.id)
      logger.warn(
        s"While analysing products to import: more matched products found for product $update. Picking the first one. Matched product ids: ${matchedIds
          .mkString(", ")}",
      )
    }
    matchableProducts.headOption
  }

  private def extractGenericProductPerRow(
      row: EnrichedDataRow,
      productType: ArticleType,
      isVariantOfProductId: Option[UUID],
      brands: Seq[BrandUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[EnrichedDataRow] = {
    val brandId = findBrandPerRow(row, brands).flatMap(_.id)
    val validName = extractName(row)
    val validDescription = extractDescription(row)
    val validPrice = extractPrice(row)
    val validCost = extractCost(row)
    val validUnit = extractUnit(row)
    val validUpc = if (productType.isStorable) extractUpc(row) else MultipleExtraction.success(None)
    val validSku = if (productType.isStorable) extractSku(row) else MultipleExtraction.success(None)
    val validArticleIdentifier = extractArticleIdentifier(row, productType)
    val validActive = extractActive(row)
    val validTrackInventory = extractTrackInventory(row)
    MultipleExtraction.combine(
      validName,
      validDescription,
      validPrice,
      validCost,
      validUnit,
      validUpc,
      validSku,
      validArticleIdentifier,
      validActive,
      validTrackInventory,
    ) {
      case (name, description, price, cost, unit, upc, sku, articleIdentifier, active, trackInventory) =>
        val update = ArticleUpdateWithIdentifier(
          toProductUpdate(
            merchantId = importRecord.merchantId,
            name = name,
            description = description,
            price = price,
            cost = cost,
            unit = unit,
            upc = upc,
            sku = sku,
            `type` = productType,
            isVariantOfProductId = isVariantOfProductId,
            active = active,
            trackInventory = trackInventory,
            brandId = brandId,
          ),
          articleIdentifier,
        )
        row.withArticleUpdate(update)
    }
  }

  private def findBrandPerRow(row: EnrichedDataRow, brands: Seq[BrandUpdate]): Option[BrandUpdate] = {
    val names =
      row
        .data
        .view
        .filterKeys(_ == Keys.Brand)
        .toMap
        .flatMap { case (_, v) => v }
        .toSeq

    brands.find(_.name.matches(names))
  }

  private def extractName(row: EnrichedDataRow): ErrorsOr[String] =
    extractString(row, Keys.ProductName)

  private def extractDescription(row: EnrichedDataRow): ErrorsOr[Option[String]] =
    MultipleExtraction.success(row.data.get(Keys.Description).flatMap(_.headOption))

  private def extractUnit(row: EnrichedDataRow): ErrorsOr[UnitType] = {
    def asEnum(u: String): ErrorsOr[UnitType] =
      Try {
        MultipleExtraction.success(UnitType.withNameInsensitive(u))
      } getOrElse MultipleExtraction.failure(
        ValidationError(
          Some(row.rowNumber),
          s"Unrecognised 'sold by' value: [$u]",
        ),
      )

    extractString(row, Keys.Unit) match {
      case Valid(u)       => asEnum(u)
      case i @ Invalid(_) => MultipleExtraction.success(UnitType.`Unit`)
    }
  }

  private def extractUpc(row: EnrichedDataRow): ErrorsOr[Option[String]] = {
    val upc = row.data.get(Keys.Upc).flatMap(_.headOption)
    if (upc.forall(articleValidator.isValidUpc)) MultipleExtraction.success(upc)
    else
      MultipleExtraction.failure(
        ValidationError(Some(row.rowNumber), s"Invalid UPC '${upc.getOrElse("")}'. UPCs must not contain spaces."),
      )
  }

  private def extractSku(row: EnrichedDataRow): ErrorsOr[Option[String]] = {
    val sku = row.data.get(Keys.Sku).flatMap(_.headOption)
    if (sku.forall(articleValidator.isValidSku)) MultipleExtraction.success(sku)
    else
      MultipleExtraction.failure(
        ValidationError(Some(row.rowNumber), s"Invalid SKU '${sku.getOrElse("")}'. SKUs must not contain spaces."),
      )
  }

  private def extractArticleIdentifier(row: EnrichedDataRow, productType: ArticleType): ErrorsOr[String] =
    MultipleExtraction.success {
      if (productType.isStorable)
        row
          .getOrElse(Keys.VariantOptionType, Seq.empty)
          .zip(row.getOrElse(Keys.VariantOption, Seq.empty))
          .map { case (optionType, option) => optionType + ":*:" + option }
          .sorted
          .mkString(":$:")
          .toLowerCase
      else ""
    }

  private def extractActive(row: EnrichedDataRow): ErrorsOr[Option[Boolean]] = {
    val active = row.get(Keys.Active).flatMap(_.headOption.map(txt => txt.trim.toLowerCase == "yes"))
    MultipleExtraction.success(active)
  }

  private def extractPrice(row: EnrichedDataRow): ErrorsOr[BigDecimal] =
    extractBigDecimal(row, Keys.Price)

  private def extractCost(row: EnrichedDataRow): ErrorsOr[Option[BigDecimal]] =
    MultipleExtraction.success(extractBigDecimal(row, Keys.Cost).toOption)

  private def extractTrackInventory(row: EnrichedDataRow): ErrorsOr[Boolean] =
    MultipleExtraction.success(containsStockData(row))

  private def toProductUpdate(
      merchantId: UUID,
      name: String,
      description: Option[String],
      price: BigDecimal,
      cost: Option[BigDecimal],
      unit: UnitType,
      upc: Option[String],
      sku: Option[String],
      `type`: ArticleType,
      isVariantOfProductId: Option[UUID],
      active: Option[Boolean],
      trackInventory: Boolean,
      brandId: Option[UUID],
    ): ArticleUpdate = {
    val margin = cost.map(c => computeMargin(priceAmount = price, costAmount = c))
    ArticleUpdate(
      id = None,
      merchantId = Some(merchantId),
      `type` = Some(`type`),
      name = Some(name),
      description = description,
      brandId = brandId,
      priceAmount = Some(price),
      costAmount = cost,
      averageCostAmount = None,
      unit = Some(unit),
      margin = margin,
      upc = upc,
      sku = sku,
      isVariantOfProductId = isVariantOfProductId,
      hasVariant = Some(`type`.isTemplate),
      trackInventory = Some(trackInventory),
      active = active,
      applyPricingToAllLocations = Some(`type`.isStorable),
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      trackInventoryParts = None,
      hasParts = None,
      scope = Some(ArticleScope.Product),
      isCombo = Some(false),
      deletedAt = None,
    )
  }

  private def toUpdatesWithCount(
      data: Seq[EnrichedDataRow],
      productType: ArticleType,
    ): UpdatesWithCount[ArticleUpdate] = {
    val updates = data.flatMap(_.articleUpdateByType(productType))
    val toUpdateCount = updates.count(_.hasExistingRecord)
    UpdatesWithCount(
      updates = updates.map(_.update),
      toAdd = updates.size - toUpdateCount,
      toUpdate = toUpdateCount,
    )
  }
}
