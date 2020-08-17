package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class GiftCardUpsertion(
    giftCard: GiftCardUpdate,
    product: ArticleUpsertion,
    imageUploads: Option[Seq[ImageUploadUpdate]],
  ) extends UpsertionModel[GiftCardRecord]
