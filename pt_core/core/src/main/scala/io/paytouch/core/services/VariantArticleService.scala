package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.{ LocationOverridesPer, RichMap }
import io.paytouch.core.conversions.VariantArticleConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ ArticleRecord, ArticleUpdate => ProductUpdateModel }
import io.paytouch.core.data.model.upsertions.VariantArticleUpsertion
import io.paytouch.core.entities.{ ProductLocation, UserContext, VariantArticleUpdate, Product => ProductEntity }
import io.paytouch.core.expansions.ArticleExpansions
import io.paytouch.core.filters.ArticleFilters
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.ArticleValidator

import scala.concurrent._

class VariantArticleService(
    val productLocationService: ProductLocationService,
    val stockService: StockService,
    val taxRateLocationService: TaxRateLocationService,
    val variantOptionService: VariantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends VariantArticleConversions {

  protected val dao = daos.variantProductDao
  protected val validator = new ArticleValidator
  val defaultFilters = ArticleFilters()

  val itemLocationService = productLocationService

  private def enrich(
      variants: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(
      e: ArticleExpansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[ProductEntity]] = {
    val locationOverridesR =
      getLocationOverridesPerProduct(variants, locationIds)(e)

    val variantOptionsPerProductR =
      variantOptionService.findVariantOptionsByVariants(variants)

    val stockPerVariantArticleR =
      getOptionalStockPerVariantArticles(variants, locationIds)(e.withStockLevel)

    for {
      locationOverrides <- locationOverridesR
      variantOptionsPerProduct <- variantOptionsPerProductR
      stockPerVariantArticle <- stockPerVariantArticleR
    } yield fromRecordsAndOptionsToEntities(
      products = variants,
      locationOverridesPerProduct = locationOverrides,
      variantOptionsPerProduct = variantOptionsPerProduct,
      stockLevelPerProduct = stockPerVariantArticle,
    )
  }

  private def getLocationOverridesPerProduct(
      products: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(
      e: ArticleExpansions,
    )(implicit
      user: UserContext,
    ): Future[LocationOverridesPer[ArticleRecord, ProductLocation]] =
    itemLocationService
      .findAllByItemIdsAsMap(products, locationIds = locationIds)(
        withReorderAmount = e.withReorderAmount,
        withStockLevel = e.withStockLevel,
        withTaxRates = e.withTaxRates,
        withTaxRateLocations = e.withTaxRateLocations,
        withTaxRateIds = e.withTaxRateIds,
      )

  private def getOptionalStockPerVariantArticles(
      products: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(
      withStockLevel: Boolean,
    )(implicit
      user: UserContext,
    ): Future[Option[Map[ArticleRecord, BigDecimal]]] =
    if (withStockLevel)
      stockService
        .findStockLevelByVariantArticleIds(products.map(_.id), locationIds)
        .map(result => result.mapKeysToRecords(products).transform((_, v) => v.values.sum).some)
    else
      Future.successful(None)

  def findVariantsByParentIds(
      mainProductIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(
      e: ArticleExpansions,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[ProductEntity]]] =
    for {
      records <- dao.findVariantsByParentIds(mainProductIds)
      entities <- enrich(records, locationIds)(e)
    } yield entities.filter(_.isVariantOfProductId.isDefined).groupBy(_.isVariantOfProductId.get)

  def convertToVariantArticleUpdates(
      mainProductId: UUID,
      variantArticles: Seq[VariantArticleUpdate],
      parent: ProductUpdateModel,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[ProductUpdateModel]]] =
    validator.validateByIdsWithParentId(variantArticles.map(_.id), mainProductId).flatMapTraverse { _ =>
      dao.findById(mainProductId).map(parentRecord => toVariantArticleModels(parent, variantArticles, parentRecord))
    }

  def convertToVariantArticleUpsertions(
      mainProductId: UUID,
      VariantArticleUpdates: Option[Seq[VariantArticleUpdate]],
      parentModel: Option[ProductUpdateModel],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[VariantArticleUpsertion]]]] =
    (VariantArticleUpdates, parentModel) match {
      case (Some(variantArticles), Some(parent)) =>
        for {
          products <- convertToVariantArticleUpdates(mainProductId, variantArticles, parent)
          productLocations <- productLocationService.convertToItemLocationUpdates(variantArticles)
          productLocationUpdates = productLocations.getOrElse(Map.empty).values.flatMap(_.values).flatten.toSeq
          productLocationTaxRates <-
            taxRateLocationService
              .convertToProductLocationTaxRateUpdates(variantArticles, productLocationUpdates)
          productVariantOptions <- variantOptionService.convertToVariantOptionUpdates(variantArticles)
        } yield {
          val upsertion =
            Multiple.combine(products, productLocations, productLocationTaxRates, productVariantOptions) {
              case (pds, pls, pltrs, pvos) =>
                toVariantUpsertions(pds, pls, pltrs, pvos)
            }
          upsertion.map(Some(_))
        }
      case _ => Future.successful(Multiple.empty)
    }

}
