package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.RewardRedemptionConversions
import io.paytouch.core.data.daos.{ Daos, RewardRedemptionDao }
import io.paytouch.core.data.model.{
  LoyaltyRewardRecord,
  OrderRecord,
  RewardRedemptionRecord,
  RewardRedemptionUpdate => RewardRedemptionUpdateModel,
}
import io.paytouch.core.entities.{
  LoyaltyMembership,
  MerchantContext,
  RewardRedemptionCreation,
  UserContext,
  RewardRedemption => RewardRedemptionEntity,
}
import io.paytouch.core.expansions.{ NoExpansions, RewardRedemptionExpansions }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators._

import scala.concurrent._

class RewardRedemptionService(
    val loyaltyMembershipService: LoyaltyMembershipService,
    val loyaltyRewardService: LoyaltyRewardService,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends RewardRedemptionConversions {

  type Creation = RewardRedemptionCreation
  type Dao = RewardRedemptionDao
  type Entity = RewardRedemptionEntity
  type Expansions = RewardRedemptionExpansions
  type Record = RewardRedemptionRecord
  type Validator = RewardRedemptionValidator
  type Update = RewardRedemptionUpdateModel

  protected val dao = daos.rewardRedemptionDao
  protected val validator = new RewardRedemptionValidator

  def reserve(id: UUID, creation: Creation)(implicit user: UserContext): Future[ErrorsOr[Result[Entity]]] =
    validator.validateCreation(id, creation).flatMapTraverse { validLoyaltyReward =>
      convertAndCreate(id, creation, validLoyaltyReward)
    }

  private def convertAndCreate(
      id: UUID,
      creation: Creation,
      loyaltyReward: LoyaltyRewardRecord,
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] = {
    implicit val merchant = user.toMerchantContext
    for {
      (resultType, record) <- dao.upsert(fromCreationToUpdate(id, creation, loyaltyReward))
      _ <- loyaltyMembershipService.logPointsAndUpdateBalance(
        record.loyaltyMembershipId,
        Seq(toRedeemedHistoryUpdate(record)),
      )
      entity <- enrich(record)(RewardRedemptionExpansions.withLoyaltyMembership)
    } yield (resultType, entity)
  }

  def bulkCancellation(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Result[Seq[Entity]]]] =
    validator.accessByIds(ids).flatMapTraverse(rewardRedemptions => convertAndCancel(rewardRedemptions))

  private def convertAndCancel(records: Seq[Record])(implicit user: UserContext): Future[(ResultType, Seq[Entity])] = {
    implicit val merchant = user.toMerchantContext
    val updates = records.map(r => toCancelUpdate(r.id))
    for {
      results <- dao.bulkUpsert(updates)
      resultTypes = results.map { case (rt, _) => rt }
      overallResultType = resultTypes.find(_ == ResultType.Created).getOrElse(ResultType.Updated)
      updatedRecords = results.map { case (_, record) => record }
      historyPerLoyaltyMembership =
        updatedRecords
          .groupBy(_.loyaltyMembershipId)
          .transform((_, v) => v.map(toCancelHistoryUpdate))
      _ <- loyaltyMembershipService.bulkLogPointsAndUpdateBalance(historyPerLoyaltyMembership)
      entities <- enrich(updatedRecords)(RewardRedemptionExpansions.withLoyaltyMembership)
    } yield (overallResultType, entities)
  }

  def recoverRewardRedemptions(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Seq[RewardRedemptionUpdateModel]] =
    Future.successful {
      val orderId = upsertion.orderId
      upsertion.rewardRedemptions.map(fromRecoveredUpsertionToUpdate(orderId, _))
    }

  def enrich(record: Record)(e: Expansions)(implicit merchant: MerchantContext): Future[Entity] =
    enrich(Seq(record))(e).map(_.head)

  def enrich(records: Seq[Record])(e: Expansions)(implicit merchant: MerchantContext): Future[Seq[Entity]] =
    for {
      loyaltyMembershipsPerRewardRedemption <- getOptionalMembershipsPerRewardRedemption(records)(
        e.withLoyaltyMembership,
      )
      loyaltyRewardsPerRewardRedemption <- loyaltyRewardService.findPerRewardRedemption(records)
    } yield fromRecordsToEntities(records, loyaltyMembershipsPerRewardRedemption, loyaltyRewardsPerRewardRedemption)

  private def getOptionalMembershipsPerRewardRedemption(
      records: Seq[Record],
    )(
      withLoyaltyMembership: Boolean,
    )(implicit
      merchant: MerchantContext,
    ): Future[Option[Map[Record, LoyaltyMembership]]] =
    if (withLoyaltyMembership) loyaltyMembershipService.findPerRewardRedemption(records).map(Some(_))
    else Future.successful(None)

  def findByOrders(
      orders: Seq[OrderRecord],
    )(implicit
      merchant: MerchantContext,
    ): Future[Map[OrderRecord, Seq[Entity]]] =
    dao.findPerOrderIds(orders.map(_.id)).flatMap { result =>
      enrich(result)(RewardRedemptionExpansions())
        .map(_.filter(_.orderId.isDefined).groupBy(_.orderId.get).mapKeysToRecords(orders))
    }
}
