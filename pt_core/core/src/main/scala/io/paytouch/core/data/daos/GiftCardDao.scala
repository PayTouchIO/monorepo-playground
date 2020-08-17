package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.GiftCardUpsertion
import io.paytouch.core.data.tables.GiftCardsTable
import io.paytouch.core.filters.GiftCardFilters
import io.paytouch.core.utils._

class GiftCardDao(
    val articleDao: ArticleDao,
    val imageUploadDao: ImageUploadDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao
       with SlickToggleableItemDao {
  type Record = GiftCardRecord
  type Update = GiftCardUpdate
  type Filters = GiftCardFilters
  type Table = GiftCardsTable
  type Upsertion = GiftCardUpsertion

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantId(merchantId = merchantId, f.updatedSince).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, f.updatedSince).length.result)

  def queryFindAllByMerchantId(merchantId: UUID, updatedSince: Option[ZonedDateTime]) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
      )
    }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      product <- articleDao.queryUpsertion(upsertion.product)
      r @ (resultType, record) <- queryUpsert(upsertion.giftCard)
      imageUploads <- asOption(upsertion.imageUploads.map(queryUpsertImagesAndDeleteTheRest(_, record.id)))
    } yield r).pipe(runWithTransaction)

  private def queryUpsertImagesAndDeleteTheRest(imageUploads: Seq[ImageUploadUpdate], objectId: UUID) =
    imageUploadDao
      .queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(
        imageUploads,
        Seq(objectId),
        ImageUploadType.GiftCard,
      )
}
