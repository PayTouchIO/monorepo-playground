package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.VariantConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{
  ArticleRecord,
  ProductVariantOptionUpdate,
  VariantOptionTypeUpdate,
  VariantOptionUpdate,
}
import io.paytouch.core.entities.{
  UserContext,
  VariantArticleUpdate,
  ArticleUpdate => ArticleUpdateEntity,
  VariantOptionType => VariantOptionTypeEntity,
  VariantOptionWithType => VariantOptionWithTypeEntity,
}
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.VariantOptionValidator

import scala.concurrent._

class VariantService(implicit val ec: ExecutionContext, val daos: Daos) extends VariantConversions {

  val productVariantOptionDao = daos.productVariantOptionDao
  val variantOptionDao = daos.variantOptionDao
  val variantOptionTypeDao = daos.variantOptionTypeDao

  val variantOptionValidator = new VariantOptionValidator

  def convertToVariantOptionTypeUpdates(
      mainProductId: UUID,
      productUpsertion: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[VariantOptionTypeUpdate]]]] =
    productUpsertion.variants match {
      case Some(variantOptionTypes) =>
        val variantOptionTypeIds = variantOptionTypes.map(_.id)
        variantOptionValidator.validateTypeByIdsWithProductId(variantOptionTypeIds, mainProductId).mapNested { _ =>
          val updates = toVariantOptionTypeUpdates(mainProductId, variantOptionTypes)
          Some(updates)
        }
      case None => Future.successful(Multiple.empty)
    }

  def findVariantOptionTypesByProductIds(
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[VariantOptionTypeEntity]]] =
    for {
      variantOptionTypes <- variantOptionTypeDao.findByProductIds(productIds)
      variantOptions <- variantOptionDao.findByVariantOptionTypeIds(variantOptionTypes.map(_.id))
    } yield toVariantOptionTypeEntities(variantOptionTypes, variantOptions)

  def convertToVariantOptionUpdates(
      mainProductId: UUID,
      productUpsertion: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[VariantOptionUpdate]]]] =
    productUpsertion.variants match {
      case Some(variants) =>
        val variantOptionIds = variants.flatMap(_.options).map(_.id)
        val variantOptionTypeIds = variants.map(_.id)
        variantOptionValidator
          .validateOptionByIdsWithTypeIdsAndProductId(variantOptionIds, variantOptionTypeIds, mainProductId)
          .mapNested { _ =>
            val updates = toVariantOptionUpdates(mainProductId, variants)
            Some(updates)
          }
      case None => Future.successful(Multiple.empty)
    }

  def findVariantOptionsByVariants(
      variants: Seq[ArticleRecord],
    ): Future[Map[ArticleRecord, Seq[VariantOptionWithTypeEntity]]] =
    findVariantOptionsByVariantIds(variants.map(_.id)).map(_.mapKeysToRecords(variants))

  def findVariantOptionsByVariantIds(variantIds: Seq[UUID]): Future[Map[UUID, Seq[VariantOptionWithTypeEntity]]] =
    productVariantOptionDao.findVariantOptionsByVariantIdsGroupedByVariantIds(variantIds)

  def convertToVariantOptionUpdates(
      variantArticles: Seq[VariantArticleUpdate],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ProductVariantOptionUpdate]]]] =
    Future.successful(Multiple.success(Some(toProductVariantOptionUpdates(variantArticles))))
}
