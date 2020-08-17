package io.paytouch.core.clients.aws

import scala.concurrent._
import scala.concurrent.duration._

import awscala.sqs.{ Message, Queue, SQSClient => AWSSQSClient }

import io.paytouch.core.json.JsonSupport
import io.paytouch.core.messages.entities.{ PtCoreMsg, PtDeliveryMsg, PtNotifierMsg, PtOrderingMsg, SQSMessage }
import io.paytouch.core.ServiceConfigurations._

class SQSSender(implicit val ec: ExecutionContext) extends SQSCommons with JsonSupport {
  private lazy val ptCoreQueues: Seq[Queue] = ptCoreQueueNames.map(retrieveQueue)

  private lazy val ptNotifierQueues: Seq[Queue] = ptNotifierQueueNames.map(retrieveQueue)

  private lazy val ptDeliveryQueues: Seq[Queue] = ptDeliveryQueueNames.map(retrieveQueue)

  private lazy val ptOrderingQueues: Seq[Queue] = ptOrderingQueueNames.map(retrieveQueue)

  def retrieveQueuesOfInterest(msg: SQSMessage[_]): Seq[Queue] =
    msg match {
      case _: PtNotifierMsg[_] => ptNotifierQueues
      case _: PtCoreMsg[_]     => ptCoreQueues
      case _: PtDeliveryMsg[_] => ptDeliveryQueues
      case _: PtOrderingMsg[_] => ptOrderingQueues
      case _                   => Seq.empty
    }

  def send(queue: Queue, msg: SQSMessage[_]): Future[Unit] = {
    val text = fromJsonToString(snakeKeys(fromEntityToJValue(msg)))
    sendMsg(queue, text)
  }

  private def sendMsg(queue: Queue, msg: String): Future[Unit] = Future(sqsClient.sendMessage(queue, msg))

}

class SQSReceiver(
    queueName: String,
    count: Int,
    wait: Duration = 5.seconds,
  )(implicit
    val ec: ExecutionContext,
  ) extends SQSCommons
       with JsonSupport {
  private lazy val queue: Queue = retrieveQueue(queueName)

  def deleteMessage(msg: Message): Future[Unit] = Future(sqsClient.delete(msg))

  def receiveMessages(): Future[Seq[Message]] =
    Future {
      sqsClient.receiveMessage(queue, count = count, wait = wait.toSeconds.toInt)
    }
}

trait SQSCommons {
  protected lazy val sqsClient = new AWSSQSClient

  protected def retrieveQueue(name: String): Queue =
    sqsClient.queue(name) getOrElse {
      throw new IllegalStateException(s"Queue $name does not exist")
    }
}
