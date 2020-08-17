package io.paytouch.core.async.sqs

import java.util.UUID

import akka.actor.{ ActorSystem, Props }
import io.paytouch.core.ServiceConfigurations._
import io.paytouch.core.messages.entities.SQSMessage
import io.paytouch.core.services.Services
import scala.concurrent._

trait SQSConsumers { self: Services =>
  def system: ActorSystem

  lazy val processors: PartialFunction[SQSMessage[_], Future[Unit]] =
    Seq(
      prepareCashDrawerActivityProcessor,
      prepareOrderReceiptProcessor,
      loyaltyProgramChangedProcessor,
      loyaltyMembershipChangedProcessor,
      giftCardChangedProcessor,
      giftCardPassChangedProcessor,
      orderSyncedProcessor,
      prepareLoyaltyProgramSignedUpProcessor,
      prepareGiftCardPassReceiptRequestedProcessor,
      imagesProcessor,
      storeCreatedProcessor,
      storesActiveChangedProcessor,
      rapidoChangedProcessor,
    ).foldLeft(PartialFunction.empty[SQSMessage[_], Future[Unit]])(_ orElse _.execute)

  lazy val startSqsMessageConsumer =
    ptCoreQueueNames.map { ptCoreQueueName =>
      system
        .actorOf(Props(new SQSMessageConsumer(ptCoreQueueName)(processors)), s"sqs-message-consumer-${UUID.randomUUID}")
    }
}
