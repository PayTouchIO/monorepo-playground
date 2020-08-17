package io.paytouch.core.services

import java.time.ZonedDateTime
import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import io.paytouch.core.conversions.LoyaltyRewardConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{
  OrderRecord,
  RewardRedemptionRecord,
  LoyaltyRewardUpdate => LoyaltyRewardUpdateModel,
}
import io.paytouch.core.entities.{
  Id,
  LoyaltyRewardUpdate,
  MerchantContext,
  Pagination,
  ProductsAssignment,
  UserContext,
  LoyaltyReward => LoyaltyRewardEntity,
}
import io.paytouch.core.expansions.ArticleExpansions
import io.paytouch.core.utils.FindResult.FindResult
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.LoyaltyRewardValidator

import scala.concurrent._

class LoyaltyRewardService(
    val loyaltyProgramService: LoyaltyProgramService,
    val loyaltyRewardProductService: LoyaltyRewardProductService,
    val articleService: ArticleService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LoyaltyRewardConversions {

  type Entity = LoyaltyRewardEntity

  protected val dao = daos.loyaltyRewardDao
  protected val validator = new LoyaltyRewardValidator

  def assignProducts(
      loyaltyRewardId: UUID,
      assignment: ProductsAssignment,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    validator.accessOneById(loyaltyRewardId).flatMap {
      case Valid(_) =>
        loyaltyRewardProductService.associateLoyaltyRewardToProducts(loyaltyRewardId, assignment.productIds)
      case i @ Invalid(_) => Future.successful(i)
    }

  def listProducts(
      loyaltyRewardId: UUID,
      updatedSince: Option[ZonedDateTime],
    )(implicit
      user: UserContext,
      pagination: Pagination,
    ): Future[FindResult[Id]] =
    loyaltyRewardProductService.listProducts(loyaltyRewardId, updatedSince)

  def convertToLoyaltyRewardUpdates(
      loyaltyProgramId: UUID,
      rewards: Option[Seq[LoyaltyRewardUpdate]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[LoyaltyRewardUpdateModel]]]] =
    rewards.fold(Future.successful(Multiple.empty[Seq[LoyaltyRewardUpdateModel]])) { rewards =>
      validator.validateByIds(rewards.map(_.id)).mapNested { _ =>
        val updateModels = rewards.map(convertToLoyaltyRewardUpdate(loyaltyProgramId, _))
        Some(updateModels)
      }
    }

  def convertToLoyaltyRewardUpdate(
      loyaltyProgramId: UUID,
      rewardUpdate: LoyaltyRewardUpdate,
    )(implicit
      user: UserContext,
    ): LoyaltyRewardUpdateModel =
    LoyaltyRewardUpdateModel(
      id = Some(rewardUpdate.id),
      merchantId = Some(user.merchantId),
      loyaltyProgramId = Some(loyaltyProgramId),
      `type` = rewardUpdate.`type`,
      amount = rewardUpdate.amount,
    )

  def findById(loyaltyRewardId: UUID)(implicit merchant: MerchantContext): Future[Option[Entity]] =
    dao.findById(loyaltyRewardId)

  def findByLoyaltyProgramIds(
      loyaltyProgramIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[Entity]]] =
    dao.findByLoyaltyProgramIds(loyaltyProgramIds).map { loyaltyRewards =>
      implicit val m: MerchantContext = user.toMerchantContext
      loyaltyRewards.groupBy(_.loyaltyProgramId).transform((_, v) => toSeqEntity(v))
    }

  def findByLoyaltyProgramId(loyaltyProgramId: UUID)(implicit user: UserContext): Future[Seq[Entity]] =
    findByLoyaltyProgramIds(Seq(loyaltyProgramId)).map(_.getOrElse(loyaltyProgramId, Seq.empty))

  def findPerRewardRedemption(
      records: Seq[RewardRedemptionRecord],
    )(implicit
      merchant: MerchantContext,
    ): Future[Map[RewardRedemptionRecord, LoyaltyRewardEntity]] =
    dao.findByIds(records.map(_.loyaltyRewardId)).map { loyaltyRewardRecords =>
      records.flatMap { record =>
        loyaltyRewardRecords
          .find(_.id == record.loyaltyRewardId)
          .map(loyaltyRewardRecord => record -> fromRecordToEntity(loyaltyRewardRecord))
      }.toMap
    }
}
