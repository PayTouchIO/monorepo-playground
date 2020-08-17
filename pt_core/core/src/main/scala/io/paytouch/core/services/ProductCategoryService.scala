package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.conversions.ProductCategoryConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ProductCategoryRecord
import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.entities.{ ArticleUpdate => ArticleUpdateEntity, _ }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ProductCategoryValidator

import scala.concurrent._

class ProductCategoryService(val ptOrderingClient: PtOrderingClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends ProductCategoryConversions {

  protected val dao = daos.productCategoryDao

  protected val validator = new ProductCategoryValidator(ptOrderingClient)
  val systemCategoryValidator = validator.systemCategoryValidator
  val articleValidator = validator.articleValidator

  def convertToProductSystemCategoryUpdates(
      mainProductId: UUID,
      productUpsertion: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ProductCategoryUpsertion]]]] =
    productUpsertion.categoryIds match {
      case Some(categoryIds) =>
        systemCategoryValidator.filterValidByIds(categoryIds).map { systemCategories =>
          val validCategories = toProductCategoryUpsertions(user.merchantId, mainProductId, systemCategories.map(_.id))
          Multiple.successOpt(validCategories)
        }
      case None => Future.successful(Multiple.empty)
    }

  def associateCategoryToProducts(
      categoryId: UUID,
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    articleValidator.accessByIds(productIds).flatMapTraverse { _ =>
      val upsertions = toProductCategoryUpsertions(user.merchantId, productIds, Seq(categoryId))
      dao.bulkUpsertAndDeleteTheRestByCategoryId(upsertions, categoryId).void
    }

  def associateCatalogCategoryToProducts(
      categoryId: UUID,
      assignments: Seq[CatalogCategoryProductAssignment],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] = {
    val productIds = assignments.map(_.productId)
    articleValidator.accessByIds(productIds).flatMapTraverse { _ =>
      val upsertions = toProductCategoryUpsertions(assignments, categoryId)
      dao.bulkUpsertAndDeleteTheRestByCategoryId(upsertions, categoryId).void
    }
  }

  def updateOrdering(
      categoryId: UUID,
      ordering: Seq[EntityOrdering],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.validateProductCategoryOrdering(categoryId, ordering).flatMapTraverse { _ =>
      val orderingUpdates = toProductCategoryOrdering(categoryId, ordering)
      dao.updateOrdering(orderingUpdates)
    }

  def findPerProduct(productIds: Seq[UUID]): Future[Map[UUID, Seq[ProductCategoryRecord]]] =
    dao.findByProductIds(productIds).map(_.groupBy(_.productId))

  def findSystemCategoryPerProduct(productIds: Seq[UUID]): Future[Map[UUID, Seq[ProductCategoryRecord]]] =
    dao.findByProductIds(productIds, legacySystemCategoriesOnly = Some(true)).map(_.groupBy(_.productId))
}
