package io.paytouch.ordering.async.sqs

import java.util.UUID

import akka.actor.{ ActorSystem, Props }
import io.paytouch.ordering.ServiceConfigurations._
import io.paytouch.ordering.messages.entities.PtOrderingMsg
import io.paytouch.ordering.services.Services
import scala.concurrent._

trait SQSConsumer { self: Services =>
  lazy val processors: PartialFunction[PtOrderingMsg[_], Future[Unit]] =
    Seq(
      merchantChangedProcessor,
    ).foldLeft(PartialFunction.empty[PtOrderingMsg[_], Future[Unit]])(_ orElse _.execute)

  def startSqsMessageConsumer(implicit system: ActorSystem) =
    system
      .actorOf(
        Props(new SQSMessageConsumer(ptOrderingQueueName)(processors)),
        s"sqs-message-consumer-${UUID.randomUUID}",
      )
}
