package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.data.model.{ ProductCategoryOptionUpdate, ProductCategoryOrdering, ProductCategoryUpdate }
import io.paytouch.core.entities.{ CatalogCategoryProductAssignment, EntityOrdering, UserContext }

trait ProductCategoryConversions {

  def toProductCategoryUpsertions(
      merchantId: UUID,
      productIds: Seq[UUID],
      categoryIds: Seq[UUID],
    ): Seq[ProductCategoryUpsertion] =
    productIds.flatMap(toProductCategoryUpsertions(merchantId, _, categoryIds))

  def toProductCategoryUpsertions(
      merchantId: UUID,
      productId: UUID,
      categoryIds: Seq[UUID],
    ): Seq[ProductCategoryUpsertion] =
    categoryIds.map(toProductCategoryUpsertion(merchantId, productId, _))

  def toProductCategoryUpsertion(
      merchantId: UUID,
      productId: UUID,
      categoryId: UUID,
    ): ProductCategoryUpsertion =
    ProductCategoryUpsertion(
      productCategory = toProductCategoryUpdate(merchantId, productId, categoryId),
      productCategoryOption = None,
    )

  def toProductCategoryUpdate(
      merchantId: UUID,
      productId: UUID,
      categoryId: UUID,
    ): ProductCategoryUpdate =
    ProductCategoryUpdate(
      id = None,
      merchantId = Some(merchantId),
      productId = Some(productId),
      categoryId = Some(categoryId),
      position = None,
    )

  def toProductCategoryOrdering(categoryId: UUID, ordering: Seq[EntityOrdering]) =
    ordering.map(o => ProductCategoryOrdering(o.id, categoryId, o.position))

  def toProductCategoryUpsertions(
      assignments: Seq[CatalogCategoryProductAssignment],
      categoryId: UUID,
    )(implicit
      user: UserContext,
    ): Seq[ProductCategoryUpsertion] =
    assignments.map(toProductCategoryUpsertion(_, categoryId))

  def toProductCategoryUpsertion(
      assignment: CatalogCategoryProductAssignment,
      categoryId: UUID,
    )(implicit
      user: UserContext,
    ): ProductCategoryUpsertion = {
    val productCategoryId = UUID.randomUUID
    val productCategoryUpdate =
      toProductCategoryUpdate(user.merchantId, assignment.productId, categoryId).copy(id = Some(productCategoryId))
    ProductCategoryUpsertion(
      productCategory = productCategoryUpdate,
      productCategoryOption = Some(toProductCategoryOptionUpdate(productCategoryId, assignment)),
    )
  }

  def toProductCategoryOptionUpdate(
      productCategoryId: UUID,
      assignment: CatalogCategoryProductAssignment,
    )(implicit
      user: UserContext,
    ) =
    ProductCategoryOptionUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productCategoryId = Some(productCategoryId),
      deliveryEnabled = Some(assignment.deliveryEnabled),
      takeAwayEnabled = Some(assignment.takeAwayEnabled),
    )
}
