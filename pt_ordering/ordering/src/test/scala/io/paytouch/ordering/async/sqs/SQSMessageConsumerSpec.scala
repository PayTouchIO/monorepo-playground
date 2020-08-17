package io.paytouch.ordering.async.sqs

import java.util.UUID

import akka.actor.Props
import awscala.s3.Bucket
import awscala.sqs.Message
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.clients.aws.SQSReceiver
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.messages.entities._

import scala.concurrent._

class SQSMessageConsumerSpec extends SQSSpec with LazyLogging {

  abstract class SQSMessageConsumerSpecContext extends SQSSpecContext {
    lazy val mockSQSClient = mock[SQSReceiver]

    lazy val mockAction = mock[Bucket] // random mock here
    mockAction.name returns genString.instance

    val queueName = "my-queue-uri"

    lazy val successProcessor: PartialFunction[SQSMessage[_], Future[Unit]] = {
      case _ =>
        val name = mockAction.name
        Future {
          logger.info(s"....mocking some random action - $name...")
        }
    }

    lazy val failureProcessor: PartialFunction[SQSMessage[_], Future[Unit]] = {
      case _ =>
        val name = mockAction.name
        Future.failed(new RuntimeException(s"mock failure of $name"))
    }

    lazy val consumer = senderSystem.actorOf(Props(new SQSMessageConsumer(queueName)(successProcessor) {
      override lazy val sqsClient = mockSQSClient
    }))

    def assertMessageIsProcessedSuccess[T <: AnyRef](msg: T) = {
      val body = fromJsonToString(snakeKeys(fromEntityToJValue(msg)))

      val mockMessage = mock[Message]
      mockMessage.id returns "message-id"
      mockMessage.body returns body

      mockSQSClient.receiveMessages().returns(Future.successful(Seq(mockMessage)), Future.successful(Seq.empty))
      mockSQSClient.deleteMessage(any).returns(Future.unit)

      consumer

      afterAWhile {
        there was atLeastOne(mockSQSClient).receiveMessages()
        there was one(mockAction).name
        there was one(mockSQSClient).deleteMessage(mockMessage)
      }
    }

    def assertMessageIsProcessedFailure[T <: AnyRef](msg: T) = {
      val body = fromJsonToString(snakeKeys(fromEntityToJValue(msg)))

      val mockMessage = mock[Message]
      mockMessage.id returns "message-id"
      mockMessage.body returns body

      mockSQSClient.receiveMessages().returns(Future.successful(Seq(mockMessage)), Future.successful(Seq.empty))

      senderSystem.actorOf(Props(new SQSMessageConsumer(queueName)(failureProcessor) {
        override lazy val sqsClient = mockSQSClient
      }))

      afterAWhile {
        there was atLeastOne(mockSQSClient).receiveMessages()
        there was one(mockAction).name
        there was no(mockSQSClient).deleteMessage(mockMessage)
      }
    }
  }

  "SQSMessageConsumer" should {

    "receive messages" in new SQSMessageConsumerSpecContext {
      mockSQSClient.receiveMessages().returns(Future.successful(Seq.empty))

      consumer

      afterAWhile(there was atLeastOne(mockSQSClient).receiveMessages())
    }

    "delete unknown message" in new SQSMessageConsumerSpecContext {
      val mockMessage = mock[Message]
      mockMessage.id returns "message-id"
      mockMessage.body returns """{ "key": "value" }"""

      mockSQSClient.receiveMessages().returns(Future.successful(Seq(mockMessage)), Future.successful(Seq.empty))
      mockSQSClient.deleteMessage(any).returns(Future.unit)

      consumer

      afterAWhile {
        there was atLeastOne(mockSQSClient).receiveMessages()
        there was noCallsTo(mockAction)
        there was one(mockSQSClient).deleteMessage(mockMessage)
      }
    }

    "process and delete message of type MerchantChanged" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID
      val msg = MerchantChanged(
        merchantId,
        MerchantChangedData(
          displayName = genString.instance,
          paymentProcessor = PaymentProcessor.Stripe,
        ),
        orderingPaymentPaymentProcessorConfigUpsertion = None,
      )
      assertMessageIsProcessedSuccess(msg)
    }
  }
}
