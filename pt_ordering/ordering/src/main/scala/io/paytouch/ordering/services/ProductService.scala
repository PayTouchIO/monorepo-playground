package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import akka.http.scaladsl.model.headers.Authorization

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities.{ ApiResponse, Merchant }
import io.paytouch.ordering._

class ProductService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends LazyLogging {
  import ProductService._

  type Entity = Product
  type Filters = (Seq[UUID] withTag Catalog, UUID withTag Location)
  type Expansions = ProductExpansions
  type Id = (UUID withTag Product, Option[UUID] withTag Merchant)

  def findAll(
      filters: Filters,
      merchantId: UUID,
      maybeExpansions: Option[ProductExpansions],
    ): Future[Seq[Entity]] = {
    implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
    val (catalogIds, locationId) = filters
    val expansions = maybeExpansions.getOrElse(
      ProductExpansions
        .empty
        .withCategoryData
        .withModifiers
        .withStockLevel
        .withTaxRates
        .withVariants,
    )
    ptCoreClient.productsList(catalogIds, locationId, expansions).map {
      case Right(ApiResponse(data, _)) => data
      case Left(error) =>
        val className = this.getClass.getSimpleName
        val errorMsg =
          s"""Error while performing findAll for $className
             |(params: filters $filters and merchant $merchantId).
             |Returning empty sequence. [${error.uri} -> ${error.errors}]""".stripMargin
        logger.error(errorMsg)
        Seq.empty
    }
  }

  def findAllPerRelId(ids: Seq[RelId], expansions: ProductExpansions): Future[Map[RelId, Seq[Entity]]] = {
    val catalogIds = ids.map(_.catalogId)
    val locationIds = ids.flatMap(_.locationId).distinct
    val merchantIds = ids.flatMap(_.merchantId).distinct

    if (catalogIds.nonEmpty && locationIds.nonEmpty && merchantIds.nonEmpty) {
      val merchantId = merchantIds.head // TODO - can we improve this?
      val locationId = locationIds.head // TODO - can we improve this?
      val filters = (catalogIds.taggedWith[Catalog], locationId.taggedWith[Location])
      findAll(filters, merchantId, expansions.some).map(products => groupProductsByRelIds(products, ids))
    }
    else {
      val description = s"catalogIds -> $catalogIds; locationId -> $locationIds; merchantIds -> $merchantIds"

      val errorMsg =
        s"""|Data missing from GraphQLContext!
            |Expected to find at least some catalogIds, a locationId and a merchantId.
            |Found: $description""".stripMargin

      logger.error(errorMsg)
      Future.successful(Map.empty)
    }
  }

  private def groupProductsByRelIds(products: Seq[Entity], relIds: Seq[RelId]): Map[RelId, Seq[Entity]] =
    relIds.map(relId => relId -> filterProductsByRelId(products, relId)).toMap

  private def filterProductsByRelId(products: Seq[Entity], relId: RelId): Seq[Entity] =
    products
      .filter(p => belongsToCategory(p, relId.categoryId) && belongsToLocationId(p, relId.locationId))
      .distinctBy(_.id)

  private def belongsToCategory(product: Product, categoryId: UUID): Boolean =
    product.categoryPositions.exists(_.categoryId == categoryId)

  private def belongsToLocationId(product: Product, locationId: Option[UUID]): Boolean =
    product.locationOverrides.keys.exists(locationId.contains)

  def findAllPerId(ids: Seq[Id]): Future[Map[Id, Entity]] = {
    val allProductIds = ids.map { case (p, _) => p }.distinct
    val merchantIds = ids.flatMap { case (_, m) => m }.distinct

    if (allProductIds.nonEmpty && merchantIds.nonEmpty) {
      val merchantId = merchantIds.head
      implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
      ptCoreClient.productsListByIds(allProductIds).map {
        case Right(ApiResponse(data, _)) =>
          data.map(p => (p.id.taggedWith[Product], Some(merchantId).taggedWith[Merchant]) -> p).toMap
        case Left(error) =>
          val className = this.getClass.getSimpleName
          val errorMsg =
            s"""Error while performing findAll for $className
               |(params: ids[]=$ids and merchant $merchantId).
               |Returning empty sequence. [${error.uri} -> ${error.errors}]""".stripMargin
          logger.error(errorMsg)
          Map.empty
      }
    }
    else {
      val description = s"productIds -> $allProductIds; merchantIds -> $merchantIds"
      val errorMsg =
        s"""Data missing from GraphQLContext!
           |Expected to find at least a productId and merchantId.
           |Found: $description""".stripMargin
      logger.error(errorMsg)
      Future.successful(Map.empty)
    }
  }

  def findById(
      id: UUID,
      merchantId: UUID,
      expansions: ProductExpansions,
    ): Future[Option[Entity]] = {
    implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
    ptCoreClient.productsGet(id, expansions).map(_.toOption.map(_.data))
  }

}

object ProductService {
  case class RelId(
      categoryId: UUID withTag Category,
      catalogId: UUID withTag Catalog,
      locationId: Option[UUID] withTag Location,
      merchantId: Option[UUID] withTag Merchant,
      expansions: ProductExpansions,
    )
}
