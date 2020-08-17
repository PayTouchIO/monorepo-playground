package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.RichMap
import io.paytouch.core.{ LocationOverridesPer, TaxRateIdsPerLocation, TaxRatesPerLocation }
import io.paytouch.core.conversions.ProductLocationConversions
import io.paytouch.core.data.daos.{ Daos, ProductLocationDao }
import io.paytouch.core.data.model.{
  ArticleRecord,
  ProductLocationRecord,
  ProductLocationUpdate => ProductLocationUpdateModel,
}
import io.paytouch.core.entities.{ ArticleLocationUpdate => ProductLocationUpdateEntity, _ }
import io.paytouch.core.expansions.TaxRateExpansions
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.{ ArticleValidator, KitchenValidator }

import scala.concurrent._

class ProductLocationService(
    val stockService: StockService,
    val taxRateService: TaxRateService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends ItemLocationService
       with ProductLocationConversions {

  type Dao = ProductLocationDao
  type Record = ProductLocationRecord

  protected val dao = daos.productLocationDao

  val articleValidator = new ArticleValidator
  val kitchenValidator = new KitchenValidator

  def accessItemById(id: UUID)(implicit user: UserContext) = articleValidator.accessOneById(id)

  def convertToItemLocationUpdates(
      productId: UUID,
      locationOverrides: Map[UUID, Option[ProductLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[ProductLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq
    val kitchenIds = locationOverrides.values.flatMap(_.flatMap(_.routeToKitchenId)).toSeq

    val validatedValues = for {
      validLocations <- locationValidator.accessByIds(locationIds)
      validKitchens <- kitchenValidator.accessByIds(kitchenIds)
    } yield Multiple.combine(validLocations, validKitchens) { case x => x }

    validatedValues.flatMapTraverse {
      case _ =>
        dao.findByItemIdAndLocationIds(productId, locationIds).map { existingProductLocations =>
          toProductLocationUpdateMap(productId, locationOverrides, existingProductLocations)
        }
    }
  }

  def convertToItemLocationUpdates(
      variantArticleUpdates: Seq[VariantArticleUpdate],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Map[UUID, Option[ProductLocationUpdateModel]]]]] = {
    val locationIds = variantArticleUpdates.flatMap(_.locationOverrides.keys)
    val productIds = variantArticleUpdates.map(_.id)
    locationValidator.validateByIds(locationIds).flatMapTraverse { _ =>
      dao.findByItemIdsAndLocationIds(productIds, locationIds).map { existingProductLocations =>
        variantArticleUpdates.map { variantArticleUpdate =>
          val productId = variantArticleUpdate.id
          val locationOverrides =
            toProductLocationUpdateMap(productId, variantArticleUpdate.locationOverrides, existingProductLocations)
          productId -> locationOverrides
        }.toMap
      }
    }
  }

  def findAllPerProduct(
      products: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[ArticleRecord, Seq[Record]]] = {
    val productIds = products.map(_.id)
    dao.findByProductIdsAndLocationIds(productIds, user.accessibleLocations(locationIds)).map { records =>
      records.groupBy(_.productId).mapKeysToRecords(products)
    }
  }

  def findAllByItemIdsAsMap(
      items: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(
      withReorderAmount: Boolean,
      withStockLevel: Boolean,
      withTaxRates: Boolean,
      withTaxRateLocations: Boolean,
      withTaxRateIds: Boolean,
    )(implicit
      user: UserContext,
    ): Future[LocationOverridesPer[ArticleRecord, ProductLocation]] = {
    val locations = user.accessibleLocations(locationIds)

    for {
      itemLocations <- dao.findByItemIdsAndLocationIds(items.map(_.id), locations)
      stockPerProduct <- getOptionalStockPerProducts(items, locationIds)(withReorderAmount, withStockLevel)
      (taxRates, taxRateIds) <- getOptionalTaxRatesPerProduct(items)(withTaxRates, withTaxRateLocations, withTaxRateIds)
    } yield groupLocationsPerItem(
      itemLocations,
      stockPerProduct,
      taxRates,
      taxRateIds,
      locations,
    ).mapKeysToRecords(items)
  }

  private def groupLocationsPerItem(
      itemLocations: Seq[Record],
      stockByItem: Option[Map[UUID, Map[UUID, Stock]]],
      taxRatesByItem: Option[Map[UUID, TaxRatesPerLocation]],
      taxRateIdsByItem: Option[Map[UUID, TaxRateIdsPerLocation]],
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): LocationOverridesPer[UUID, ProductLocation] =
    itemLocations.groupBy(_.itemId).map {
      case (itemId, itemsLocs) =>
        val stock = stockByItem.flatMap(_.get(itemId)).getOrElse(Map.empty)
        val taxRates = taxRatesByItem.flatMap(_.get(itemId)).getOrElse(Map.empty)
        val taxRateIds = taxRateIdsByItem.flatMap(_.get(itemId)).getOrElse(Map.empty)
        itemId -> fromItemLocationsToProductLocations(itemsLocs, stock, taxRates, taxRateIds).toMap
    }

  private def getOptionalTaxRatesPerProduct(
      products: Seq[ArticleRecord],
    )(
      withTaxRates: Boolean,
      withTaxRateLocations: Boolean,
      withTaxRateIds: Boolean,
    )(implicit
      user: UserContext,
    ): Future[(Option[Map[UUID, TaxRatesPerLocation]], Option[Map[UUID, TaxRateIdsPerLocation]])] =
    if (withTaxRates || withTaxRateIds || withTaxRateLocations) {
      val taxRateExpansion = TaxRateExpansions(withLocations = withTaxRateLocations)
      val allItemIds = products.map(_.id) ++ products.flatMap(_.isVariantOfProductId)
      taxRateService.findByProductIds(allItemIds)(taxRateExpansion).map { taxRatesPerTemplateProduct =>
        val taxRatesPerVariantProduct: Map[UUID, TaxRatesPerLocation] = products
          .filter(p => p.`type`.isVariant && p.isVariantOfProductId.isDefined)
          .map { product =>
            product.id -> taxRatesPerTemplateProduct.getOrElse(product.isVariantOfProductId.get, Map.empty)
          }
          .toMap
        val taxRatesPerTemplateAndVariantProduct = taxRatesPerTemplateProduct ++ taxRatesPerVariantProduct
        val taxRates = if (withTaxRates) Some(taxRatesPerTemplateAndVariantProduct) else None

        val taxRateIds =
          if (withTaxRateIds)
            Some {
              taxRatesPerTemplateAndVariantProduct
                .transform { (_, v1) =>
                  v1.transform((_, v2) => v2.map(_.id))
                }
            }
          else
            None

        (taxRates, taxRateIds)
      }
    }
    else Future.successful((None, None))

  def findAllByProductIdsAndLocationIds(productIds: Seq[UUID], locationIds: Seq[UUID]): Future[Seq[Record]] =
    dao.findByProductIdsAndLocationIds(productIds, locationIds)

  def updateAverageCosts(productAverageCostsByLocation: Map[UUID, BigDecimal], locationId: UUID) =
    dao.updateAverageCosts(productAverageCostsByLocation, locationId)

  def findPriceRangesByProductIds(
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, MonetaryRange]] =
    dao
      .findPriceRangesByProductIds(productIds, user.defaultedToUserLocations(locationIds))
      .map(_.transform {
        case (_, (min, max)) =>
          MonetaryRange(
            min = MonetaryAmount(min),
            max = MonetaryAmount(max),
          )
      })

  def findCostRangesByProductIds(
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, MonetaryRange]] =
    dao
      .findCostRangesByProductIds(productIds, user.defaultedToUserLocations(locationIds))
      .map(_.transform {
        case (_, (min, max)) =>
          MonetaryRange(min = MonetaryAmount(min), max = MonetaryAmount(max))
      })

  private def getOptionalStockPerProducts(
      products: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(
      withReorderAmount: Boolean,
      withStockLevel: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[UUID, Map[UUID, Stock]]]] =
    if (withReorderAmount || withStockLevel) {
      val ids = products.map(_.id)
      stockService.findStockByArticleIds(ids, locationIds).map(result => Some(result))
    }
    else
      Future.successful(None)
}
