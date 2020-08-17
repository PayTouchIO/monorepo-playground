package io.paytouch.core.services

import io.paytouch.core.RichMap
import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.TaxRateLocationConversions
import io.paytouch.core.data.daos.{ Daos, TaxRateLocationDao }
import io.paytouch.core.data.model.{
  ProductLocationTaxRateUpdate,
  TaxRateLocationRecord,
  ProductLocationUpdate => ProductLocationUpdateModel,
  TaxRateLocationUpdate => TaxRateLocationUpdateModel,
}
import io.paytouch.core.entities.{
  ArticleUpdate => ArticleUpdateEntity,
  TaxRateLocationUpdate => TaxRateLocationUpdateEntity,
  VariantArticleUpdate => VariantArticleUpdateEntity,
  _,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.{ TaxRateLocationValidator, TaxRateValidator }

import scala.concurrent._

class TaxRateLocationService(implicit val ec: ExecutionContext, val daos: Daos)
    extends ItemLocationService
       with TaxRateLocationConversions {

  type Dao = TaxRateLocationDao
  type Record = TaxRateLocationRecord

  protected val dao = daos.taxRateLocationDao
  protected val validator = new TaxRateLocationValidator

  val taxRateValidator = new TaxRateValidator

  def accessItemById(id: UUID)(implicit user: UserContext) = taxRateValidator.accessOneById(id)

  def findAllByTaxRateIds(taxRateIds: Seq[UUID])(implicit user: UserContext): Future[Seq[Record]] =
    dao.findByItemIdsAndLocationIds(taxRateIds, user.locationIds)

  def findAllByLocationIds(locationIds: Seq[UUID])(implicit user: UserContext): Future[Seq[Record]] =
    dao.findByLocationIds(locationIds intersect user.locationIds)

  def convertToProductLocationTaxRateUpdates(
      mainProductId: UUID,
      productUpsertion: ArticleUpdateEntity,
      productLocations: Seq[ProductLocationUpdateModel],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]]]] = {
    val allLocationToTaxRateIds =
      productUpsertion.locationOverrides.transform((_, v) => v.toSeq.flatMap(_.taxRateIds))
    validator.accessLocationAndTaxRatesByIds(allLocationToTaxRateIds).mapNested { _ =>
      toProductLocationTaxRateUpdateMap(mainProductId, productUpsertion.locationOverrides, productLocations)
    }
  }

  def convertToProductLocationTaxRateUpdates(
      variantArticles: Seq[VariantArticleUpdateEntity],
      productLocations: Seq[ProductLocationUpdateModel],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Map[UUID, Option[Seq[ProductLocationTaxRateUpdate]]]]]] = {
    val allLocationOverrides = variantArticles.map(_.locationOverrides.transform { (_, v) =>
      v.toSeq.flatMap(_.taxRateIds)
    })
    val allLocationToTaxRateIds = allLocationOverrides.foldLeft(Map.empty[UUID, Seq[UUID]]) {
      case (acc, m) => acc.merge(m)(_ ++ _, Seq.empty)
    }
    validator.accessLocationAndTaxRatesByIds(allLocationToTaxRateIds).mapNested { _ =>
      variantArticles.map { variantArticle =>
        val locationOverrides = variantArticle.locationOverrides
        val prodLocs = productLocations.filter(_.productId.contains(variantArticle.id))
        variantArticle.id -> toProductLocationTaxRateUpdateMap(variantArticle.id, locationOverrides, prodLocs)
      }.toMap
    }
  }

  def convertToItemLocationUpdates(
      itemId: UUID,
      locationOverrides: Map[UUID, Option[TaxRateLocationUpdateEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Map[UUID, Option[TaxRateLocationUpdateModel]]]] = {
    val locationIds = locationOverrides.keys.toSeq
    for {
      locations <- locationValidator.validateByIds(locationIds)
      itemLocations <- dao.findByItemIdsAndLocationIds(Seq(itemId), locationIds).map(Multiple.success)
    } yield Multiple.combine(locations, itemLocations) {
      case (_, itemLocs) =>
        locationOverrides.map {
          case (locationId, itemLocationUpdate) =>
            val taxRateLocationUpdate = itemLocationUpdate.map(itemLocUpd =>
              itemLocs
                .find(_.locationId == locationId)
                .map(toTaxRateLocationUpdate)
                .getOrElse(toTaxRateLocationUpdate(itemId, locationId))
                .copy(active = itemLocUpd.active),
            )
            (locationId, taxRateLocationUpdate)
        }
    }
  }
}
