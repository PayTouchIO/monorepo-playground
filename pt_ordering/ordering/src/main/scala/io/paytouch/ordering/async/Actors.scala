package io.paytouch.ordering
package async

import java.util.UUID

import akka.actor.{ ActorRef, Props }
import io.paytouch.ordering.async.sqs.SQSMessageSender
import io.paytouch.ordering.services.Services

trait Actors { self: Services =>
  lazy val messageSender: ActorRef withTag SQSMessageSender =
    asyncSystem
      .actorOf(Props[SQSMessageSender](), s"sqs-message-sender-${UUID.randomUUID}")
      .taggedWith[SQSMessageSender]
}
