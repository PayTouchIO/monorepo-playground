package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LoyaltyProgramUpsertion
import io.paytouch.core.data.model.{ ImageUploadUpdate, LoyaltyProgramRecord, LoyaltyProgramUpdate }
import io.paytouch.core.data.tables.LoyaltyProgramsTable
import io.paytouch.core.filters.LoyaltyProgramFilters
import io.paytouch.core.utils.ResultType

import scala.concurrent._

class LoyaltyProgramDao(
    val imageUploadDao: ImageUploadDao,
    val loyaltyProgramLocationDao: LoyaltyProgramLocationDao,
    val loyaltyRewardDao: LoyaltyRewardDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao {

  type Record = LoyaltyProgramRecord
  type Update = LoyaltyProgramUpdate
  type Upsertion = LoyaltyProgramUpsertion
  type Filters = LoyaltyProgramFilters
  type Table = LoyaltyProgramsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int) =
    findAllByMerchantId(merchantId, f.locationId, f.updatedSince)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, updatedSince).sortBy(_.createdAt).drop(offset).take(limit)
    run(q.result)
  }

  def countAllWithFilters(merchantId: UUID, f: Filters) =
    countAllByMerchantId(merchantId, f.locationId, f.updatedSince)

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, updatedSince)
    run(q.length.result)
  }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val upserts = for {
      (resultType, loyaltyProgram) <- queryUpsert(upsertion.loyaltyProgram)
      _ <-
        loyaltyProgramLocationDao
          .queryBulkUpsertAndDeleteTheRestByLoyaltyProgramId(upsertion.loyaltyProgramLocations, loyaltyProgram.id)
      _ <- asOption(
        upsertion.loyaltyRewards.map(loyaltyRewardDao.queryBulkUpsertAndDeleteTheRest(_, loyaltyProgram.id)),
      )
      imageUploads <- asOption(upsertion.imageUploads.map(queryUpsertImagesAndDeleteTheRest(_, loyaltyProgram.id)))
    } yield (resultType, loyaltyProgram)
    runWithTransaction(upserts)
  }

  private def queryUpsertImagesAndDeleteTheRest(imageUploads: Seq[ImageUploadUpdate], objectId: UUID) = {
    val imgType = ImageUploadType.LoyaltyProgram
    imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(imageUploads, Seq(objectId), imgType)
  }

  def findOneActiveLoyaltyProgram(merchantId: UUID, locationId: Option[UUID]): Future[Option[Record]] = {
    val q = queryFindAllByMerchantId(merchantId, locationId, None).filter(_.active)
    run(q.result.headOption)
  }

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      updatedSince: Option[ZonedDateTime],
    ) =
    table
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          locationId.map(lId => t.id in loyaltyProgramLocationDao.queryFindByLocationId(lId).map(_.loyaltyProgramId)),
          updatedSince.map(date =>
            any(
              t.id in queryUpdatedSince(date).map(_.id),
              t.id in loyaltyRewardDao.queryUpdatedSince(date).map(_.loyaltyProgramId),
            ),
          ),
        ),
      )
      .sortBy(_.createdAt)
}
