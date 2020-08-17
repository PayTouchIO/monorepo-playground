package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ GiftCardRecord, GiftCardUpdate => GiftCardUpdateModel }
import io.paytouch.core.entities.{
  ImageUrls,
  MonetaryAmount,
  UserContext,
  GiftCard => GiftCardEntity,
  GiftCardUpdate => GiftCardUpdateEntity,
  Product => ProductEntity,
}

trait GiftCardConversions {

  def fromRecordsToEntities(
      records: Seq[GiftCardRecord],
      products: Seq[ProductEntity],
      imageUrlsPerGiftCard: Map[GiftCardRecord, Seq[ImageUrls]],
    )(implicit
      user: UserContext,
    ) =
    for {
      record <- records
      product <- products.filter(_.id == record.productId)
    } yield {
      val imageUrls = imageUrlsPerGiftCard.getOrElse(record, Seq.empty)
      fromRecordToEntity(record, product, imageUrls)
    }

  def fromRecordToEntity(
      record: GiftCardRecord,
      product: ProductEntity,
      imageUrls: Seq[ImageUrls],
    )(implicit
      user: UserContext,
    ): GiftCardEntity =
    GiftCardEntity(
      id = record.id,
      amounts = record.amounts.map(am => MonetaryAmount(am, user.currency)),
      businessName = record.businessName,
      templateDetails = record.templateDetails,
      templateCreated = record.appleWalletTemplateId.isDefined && record.androidPayTemplateId.isDefined,
      active = record.active,
      product = product,
      avatarImageUrls = imageUrls,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      productId: UUID,
      upsertion: GiftCardUpdateEntity,
    )(implicit
      user: UserContext,
    ): GiftCardUpdateModel =
    GiftCardUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      amounts = upsertion.amounts,
      businessName = upsertion.businessName,
      templateDetails = upsertion.templateDetails,
      appleWalletTemplateId = None,
      androidPayTemplateId = None,
      active = upsertion.active,
    )

}
