package io.paytouch.core.async.sqs

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import akka.actor.{ Actor, ActorLogging }
import akka.event.Logging

import awscala.sqs.Queue

import io.paytouch.core.clients.aws.SQSSender
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.messages.entities.SQSMessage

final case class SendMsgWithRetry(msg: SQSMessage[_])
final case class MsgQueueWithRetry(
    queue: Queue,
    msg: SQSMessage[_],
    tries: Int = 0,
  )

class SQSMessageSender extends Actor with ActorLogging with JsonSupport {
  import context.dispatcher

  lazy val sqsClient = new SQSSender

  val MaxTries = 20
  val RetryDelay = 30.seconds

  def receive: Receive = {
    case SendMsgWithRetry(msg)                => sendMessagePerQueueWithRetry(msg)
    case MsgQueueWithRetry(queue, msg, tries) => tryToSend(queue, msg, tries)
  }

  def sendMessagePerQueueWithRetry(msg: SQSMessage[_]) =
    sqsClient.retrieveQueuesOfInterest(msg).foreach(q => self ! MsgQueueWithRetry(q, msg, 0))

  def tryToSend(
      queue: Queue,
      msg: SQSMessage[_],
      tries: Int,
    ) =
    sqsClient.send(queue, msg) onComplete {
      case Success(_) =>
        log.info(s"SQS Message ${msg.eventName} sent to queue ${queue.url} (${tries + 1} of $MaxTries tries)")
      case Failure(throwable) => recover(queue, msg, tries + 1, throwable)
    }

  def recover(
      queue: Queue,
      msg: SQSMessage[_],
      tries: Int,
      throwable: Throwable,
    ) =
    if (tries >= MaxTries) {
      val jsonMsg = fromEntityToString(msg)
      log.error(
        s"[SQS MESSAGE - FAILED] Could not send message ${msg.eventName} to queue ${queue.url} after $tries of $MaxTries tries: giving up! The message was " + jsonMsg,
      )
    }
    else retry(queue, msg, tries, throwable)

  def retry(
      queue: Queue,
      msg: SQSMessage[_],
      tries: Int,
      throwable: Throwable,
    ) = {
    log.warning(
      s"[SQS MESSAGE - RETRY] Could not send message ${msg.eventName} to queue ${queue.url}: `$throwable`, ${Logging
        .stackTraceFor(throwable)}. Retrying in $RetryDelay... ($tries of $MaxTries tries)",
    )
    context.system.scheduler.scheduleOnce(RetryDelay, self, MsgQueueWithRetry(queue, msg, tries))
  }
}
