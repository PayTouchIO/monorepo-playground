package io.paytouch.core.processors

import scala.concurrent._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services._

class PrepareLoyaltyProgramSignedUpProcessor(
    val loyaltyMembershipService: LoyaltyMembershipService,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: PrepareLoyaltyMembershipSignedUp => processPrepareLoyaltyMembershipSignedUp(msg)
  }

  private def processPrepareLoyaltyMembershipSignedUp(msg: PrepareLoyaltyMembershipSignedUp): Future[Unit] = {
    val loyaltyMembership = msg.payload.data.loyaltyMembership
    val loyaltyProgram = msg.payload.data.loyaltyProgram
    implicit val u: MerchantContext = msg.payload.merchantContext
    loyaltyMembershipService.upsertPass(loyaltyMembership).map {
      case Some(updatedLoyaltyMembership) =>
        loyaltyMembershipService.sendLoyaltyMembershipSignedUp(updatedLoyaltyMembership, loyaltyProgram)
      case _ => ()
    }
  }
}
