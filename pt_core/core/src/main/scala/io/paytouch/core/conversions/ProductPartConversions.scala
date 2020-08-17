package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ProductPartRecord, ProductPartUpdate }
import io.paytouch.core.entities.{ ProductPart, ProductPartAssignment, UserContext, Product => ProductEntity }

trait ProductPartConversions extends ModelConversion[ProductPartAssignment, ProductPartUpdate] {

  def fromUpsertionsToUpdates(
      productId: UUID,
      updates: Seq[ProductPartAssignment],
    )(implicit
      user: UserContext,
    ): Seq[ProductPartUpdate] =
    updates.map(update => fromUpsertionToUpdate(productId, update))

  def fromUpsertionToUpdate(
      productId: UUID,
      update: ProductPartAssignment,
    )(implicit
      user: UserContext,
    ): ProductPartUpdate =
    ProductPartUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      partId = Some(update.partId),
      quantityNeeded = Some(update.quantityNeeded),
    )

  def fromRecordsAndOptionsToEntities(productParts: Seq[ProductPartRecord], parts: Seq[ProductEntity]) =
    for {
      productPart <- productParts
      part <- parts.filter(_.id == productPart.partId)
    } yield fromRecordAndOptionsToEntity(productPart, part)

  def fromRecordAndOptionsToEntity(record: ProductPartRecord, part: ProductEntity): ProductPart =
    ProductPart(part, record.quantityNeeded)
}
