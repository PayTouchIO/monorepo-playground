package io.paytouch.ordering.async.sqs

import akka.actor.{ Actor, ActorLogging }
import awscala.sqs.Queue
import io.paytouch.ordering.clients.aws.SQSSender
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.messages.entities.SQSMessage

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

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
      case Failure(_) => recover(queue, msg, tries + 1)
    }

  def recover(
      queue: Queue,
      msg: SQSMessage[_],
      tries: Int,
    ) =
    if (tries >= MaxTries) {
      val jsonMsg = fromEntityToString(msg)
      log.error(
        s"[SQS MESSAGE - FAILED] Could not send message ${msg.eventName} to queue ${queue.url} after $tries of $MaxTries tries: giving up! The message was " + jsonMsg,
      )
    }
    else retry(queue, msg, tries)

  def retry(
      queue: Queue,
      msg: SQSMessage[_],
      tries: Int,
    ) = {
    log.warning(
      s"[SQS MESSAGE - RETRY] Could not send message ${msg.eventName} to queue ${queue.url}: retrying in $RetryDelay... ($tries of $MaxTries tries)",
    )
    context.system.scheduler.scheduleOnce(RetryDelay, self, MsgQueueWithRetry(queue, msg, tries))
  }
}
