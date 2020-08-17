package io.paytouch.ordering.clients.aws

import awscala.sqs.{ Message => IncomingMessage, Queue, SQSClient => AWSSQSClient }
import io.paytouch.ordering.ServiceConfigurations._
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.messages.entities.{ PtCoreMsg, SQSMessage }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class SQSSender(implicit val ec: ExecutionContext) extends SQSCommons with JsonSupport {

  private lazy val ptCoreQueue: Queue = retrieveQueue(ptCoreQueueName)

  def retrieveQueuesOfInterest(msg: SQSMessage[_]): Seq[Queue] =
    msg match {
      case _: PtCoreMsg[_] => Seq(ptCoreQueue)
      case _               => Seq.empty
    }

  def send(queue: Queue, msg: SQSMessage[_]): Future[Unit] = {
    val text = marshalToSnakeCase(msg)
    sendMsg(queue, text)
  }

  private def sendMsg(queue: Queue, msg: String): Future[Unit] = Future(sqsClient.sendMessage(queue, msg))

}

class SQSReceiver(
    queueName: String,
    count: Int,
    wait: Duration = 5 seconds,
  )(implicit
    val ec: ExecutionContext,
  ) extends SQSCommons
       with JsonSupport {
  private lazy val queue: Queue = retrieveQueue(queueName)

  def deleteMessage(msg: IncomingMessage): Future[Unit] = Future(sqsClient.delete(msg))

  def receiveMessages(): Future[Seq[IncomingMessage]] =
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
