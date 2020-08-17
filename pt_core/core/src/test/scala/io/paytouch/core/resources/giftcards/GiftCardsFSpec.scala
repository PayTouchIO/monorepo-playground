package io.paytouch.core.resources.giftcards

import java.util.UUID

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, ImageUploadType }
import io.paytouch.core.data.model.{ GiftCardRecord, ImageUploadRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class GiftCardsFSpec extends FSpec {
  abstract class GiftCardResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val giftCardDao = daos.giftCardDao
    val articleDao = daos.articleDao
    val imageUploadDao = daos.imageUploadDao

    def assertResponse(
        entity: GiftCard,
        record: GiftCardRecord,
        images: Seq[ImageUploadRecord] = Seq.empty,
      ) = {
      entity.id ==== record.id
      entity.amounts.map(_.amount) ==== record.amounts
      entity.businessName ==== record.businessName
      entity.templateDetails ==== record.templateDetails
      entity.templateCreated ==== record.appleWalletTemplateId.isDefined && record.androidPayTemplateId.isDefined
      entity.active ==== record.active
      entity.product.id ==== record.productId
      entity.avatarImageUrls.map(_.imageUploadId) ==== images.map(_.id)
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
    }

    def assertCreation(giftCardId: UUID, creation: GiftCardCreation) =
      assertUpdate(giftCardId, creation.asUpdate)

    def assertUpdate(giftCardId: UUID, update: GiftCardUpdate) = {
      val giftCard = giftCardDao.findById(giftCardId).await.get

      if (update.amounts.isDefined) update.amounts ==== Some(giftCard.amounts)
      if (update.businessName.isDefined) update.businessName ==== Some(giftCard.businessName)
      if (update.templateDetails.isDefined) update.templateDetails ==== giftCard.templateDetails
      if (update.active.isDefined) update.active ==== Some(giftCard.active)

      val productId = giftCard.productId
      val product = articleDao.findById(productId).await.get

      product.scope ==== ArticleScope.Product
      product.`type` ==== ArticleType.GiftCard

      if (update.name.isDefined) update.name ==== Some(product.name)
      if (update.upc.isDefined) update.upc ==== product.upc
      if (update.sku.isDefined) update.sku ==== product.sku

      if (update.imageUploadIds.isDefined) {
        val images = imageUploadDao.findByObjectIds(Seq(giftCard.id)).await
        images.forall(_.objectType == ImageUploadType.GiftCard) should beTrue
        update.imageUploadIds ==== Some(images.map(_.id))
      }
    }
  }
}
