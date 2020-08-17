package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ BundleOptionRecord, BundleOptionUpdate => BundleOptionUpdateModel }
import io.paytouch.core.entities.{
  ArticleInfo,
  BundleOption,
  UserContext,
  BundleOptionUpdate => BundleOptionUpdateEntity,
}

trait BundleOptionConversions {

  def toBundleOptionUpdate(
      bundleSetId: UUID,
      upsertion: BundleOptionUpdateEntity,
      index: Int,
    )(implicit
      user: UserContext,
    ): BundleOptionUpdateModel =
    BundleOptionUpdateModel(
      id = Some(upsertion.id),
      merchantId = Some(user.merchantId),
      bundleSetId = Some(bundleSetId),
      articleId = Some(upsertion.articleId),
      priceAdjustment = Some(upsertion.priceAdjustment),
      position = Some(upsertion.position.getOrElse(index)),
    )

  def fromRecordsAndOptionsToEntities(
      records: Seq[BundleOptionRecord],
      articleInfoPerArticleId: Map[UUID, ArticleInfo],
    ): Seq[BundleOption] =
    records.flatMap { record =>
      val articleInfo = articleInfoPerArticleId.get(record.articleId)
      articleInfo.map(fromRecordAndOptionsToEntity(record, _))
    }

  def fromRecordAndOptionsToEntity(record: BundleOptionRecord, articleInfo: ArticleInfo): BundleOption =
    BundleOption(
      id = record.id,
      article = articleInfo,
      priceAdjustment = record.priceAdjustment,
      position = record.position,
    )
}
