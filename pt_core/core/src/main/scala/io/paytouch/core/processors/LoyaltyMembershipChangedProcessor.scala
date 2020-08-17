package io.paytouch.core.processors

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities._
import io.paytouch.core.services.LoyaltyMembershipService

class LoyaltyMembershipChangedProcessor(
    loyaltyMembershipService: LoyaltyMembershipService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: LoyaltyMembershipChanged => processLoyaltyMembershipChanged(msg)
  }

  private def processLoyaltyMembershipChanged(msg: LoyaltyMembershipChanged): Future[Unit] = {
    val loyaltyMembership = msg.payload.data
    implicit val u: MerchantContext = msg.payload.merchantContext
    loyaltyMembershipService.upsertPass(loyaltyMembership).void
  }
}
