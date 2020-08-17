package io.paytouch.ordering.async.sqs

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import akka.actor.{ Actor, ActorLogging }

import awscala.sqs.Message

import io.paytouch.ordering.clients.aws.SQSReceiver
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.messages.entities._
import io.paytouch.ordering.ServiceConfigurations

import scala.language.postfixOps

case object ReceiveMessages
final case class ProcessMessage(msg: Message)

class SQSMessageConsumer(queueName: String)(processor: PartialFunction[PtOrderingMsg[_], Future[Unit]])
    extends Actor
       with ActorLogging
       with JsonSupport {
  import context.dispatcher

  val SQSInfo = "[SQS Info]"
  val SQSWarning = "[SQS Warning]"

  lazy val sqsClient = new SQSReceiver(queueName, ServiceConfigurations.sqsMsgCount)

  override def preStart(): Unit =
    context.system.scheduler.scheduleAtFixedRate(Duration.Zero, 5.seconds, self, ReceiveMessages)

  def receive: Receive = {
    case ReceiveMessages => receiveMessages()
    case ProcessMessage(msg) =>
      processMessage(msg).onComplete {
        case Success(_) => deleteMessage(msg)
        case Failure(error) =>
          Future(log.warning(s"$SQSWarning Caught error processing SQS message: ${msg.id} [${msg.body}]: ${error}"))
      }
  }

  private def receiveMessages() = {
    log.debug(s"Receiving messages for queue {}...", queueName)
    sqsClient.receiveMessages().map(messages => messages.foreach(msg => self ! ProcessMessage(msg)))
  }

  private def processMessage(msg: Message): Future[Unit] = {
    log.debug(s"$SQSInfo Processing message {}...", msg)
    toPtOrderingMsg(msg) match {
      case Some(sqsMsg) => processPtOrderingMsg(msg.id, sqsMsg)
      case None         => Future(log.warning(s"$SQSWarning Could not deserialize message ${msg.id}. [${msg.body}]"))
    }
  }

  private def deleteMessage(msg: Message) =
    sqsClient.deleteMessage(msg).map(_ => log.info(s"$SQSInfo Deleted message ${msg.id} from queue $queueName"))

  private def processPtOrderingMsg(id: String, msg: PtOrderingMsg[_]): Future[Unit] = {
    log.info(s"$SQSInfo Processing message $id event ${msg.eventName}")
    val ignore: PartialFunction[PtOrderingMsg[_], Future[Unit]] = {
      case m => Future(log.warning(s"$SQSWarning Ignoring message $id: $m"))
    }
    val processF = processor orElse ignore
    processF(msg).map(_ => log.info(s"$SQSInfo Completed processing message $id"))
  }

  private def toPtOrderingMsg(msg: Message): Option[PtOrderingMsg[_]] = {
    val body = unmarshalToCamelCase(msg.body)

    def readAsOptional[T: Manifest]: Option[T] =
      Try(fromJsonToEntity[T](body)) match {
        case Success(t) => Some(t)
        case Failure(ex) =>
          log.warning(s"$SQSWarning Could not parse json: {}. Body was: $body", ex)
          None
      }

    val eventName: Option[String] =
      findKeyInJsonString(body, "eventName") match {
        case JString(name) => Some(name)
        case _ =>
          log.warning(s"$SQSWarning Could not find eventName in message ${msg.id}: [${msg.body}]")
          None
      }

    eventName.flatMap {
      case MerchantChanged.eventName => readAsOptional[MerchantChanged]
      case name                      => log.warning(s"$SQSWarning Unknown event name $name"); None
    }
  }

}
