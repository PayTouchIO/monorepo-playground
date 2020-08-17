package io.paytouch.core.async.sqs

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import akka.actor.{ Actor, ActorLogging }

import awscala.sqs.Message

import io.paytouch.core.clients.aws.SQSReceiver
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.messages.entities._
import io.paytouch.core.ServiceConfigurations

case object ReceiveMessages
final case class ProcessMessage(msg: Message)

class SQSMessageConsumer(queueName: String)(processor: PartialFunction[SQSMessage[_], Future[Unit]])
    extends Actor
       with ActorLogging
       with JsonSupport {
  import context.dispatcher

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
    toSQSMessage(msg) match {
      case Some(sqsMsg) => processSQSMessage(msg.id, sqsMsg)
      case None         => Future(log.warning(s"$SQSWarning Could not deserialize message ${msg.id}. [${msg.body}]"))
    }
  }

  private def deleteMessage(msg: Message) =
    sqsClient.deleteMessage(msg).map(_ => log.info(s"$SQSInfo Deleted message ${msg.id} from queue $queueName"))

  private def processSQSMessage(id: String, msg: SQSMessage[_]): Future[Unit] = {
    log.info(s"$SQSInfo Processing message $id event ${msg.eventName}")
    val ignore: PartialFunction[SQSMessage[_], Future[Unit]] = {
      case m => Future(log.warning(s"$SQSWarning Ignoring message $id: $m"))
    }
    val processF = processor orElse ignore
    processF(msg).map(_ => log.info(s"$SQSInfo Completed processing message $id"))
  }

  private def toSQSMessage(msg: Message): Option[SQSMessage[_]] = {
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
      case PrepareCashDrawerReport.eventName             => readAsOptional[PrepareCashDrawerReport]
      case PrepareOrderReceipt.eventName                 => readAsOptional[PrepareOrderReceipt]
      case LoyaltyProgramChanged.eventName               => readAsOptional[LoyaltyProgramChanged]
      case LoyaltyMembershipChanged.eventName            => readAsOptional[LoyaltyMembershipChanged]
      case GiftCardChanged.eventName                     => readAsOptional[GiftCardChanged]
      case GiftCardPassChanged.eventName                 => readAsOptional[GiftCardPassChanged]
      case OrderSynced.eventName                         => readAsOptional[OrderSynced]
      case PrepareGiftCardPassReceiptRequested.eventName => readAsOptional[PrepareGiftCardPassReceiptRequested]
      case PrepareLoyaltyMembershipSignedUp.eventName    => readAsOptional[PrepareLoyaltyMembershipSignedUp]
      case ImagesDeleted.eventName                       => readAsOptional[ImagesDeleted]
      case ImagesAssociated.eventName                    => readAsOptional[ImagesAssociated]
      case StoresActiveChanged.eventName                 => readAsOptional[StoresActiveChanged]
      case StoreCreated.eventName                        => readAsOptional[StoreCreated]
      case RapidoChanged.eventName                       => readAsOptional[RapidoChanged]
      case name                                          => log.warning(s"$SQSWarning Unknown event name $name"); None
    }
  }

}
