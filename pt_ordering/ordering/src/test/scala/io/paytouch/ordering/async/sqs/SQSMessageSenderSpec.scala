package io.paytouch.ordering.async.sqs

import akka.actor.Props
import awscala.sqs.Queue
import io.paytouch.ordering.clients.aws.SQSSender
import io.paytouch.ordering.messages.entities.SQSMessage

import scala.concurrent._
import scala.concurrent.duration._

class SQSMessageSenderSpec extends SQSSpec {

  abstract class SQSMessageSenderSpecContext extends SQSSpecContext {

    val mockSQSClient = mock[SQSSender]
    val testMaxRetries = 5
    val testRetryDelay = 10 milliseconds

    lazy val sender = senderSystem.actorOf(Props(new SQSMessageSender {
      override lazy val sqsClient = mockSQSClient
      override val MaxTries = testMaxRetries
      override val RetryDelay = testRetryDelay
    }))

  }

  "SQSMessageSender" should {

    "send a message in each queue" in new SQSMessageSenderSpecContext {
      val queueA = mock[Queue]
      val queueB = mock[Queue]
      val queueC = mock[Queue]

      val msg = mock[SQSMessage[_]]

      mockSQSClient.retrieveQueuesOfInterest(any) returns Seq(queueA, queueB, queueC)
      mockSQSClient.send(any, any) returns Future.unit

      sender ! SendMsgWithRetry(msg)
      afterAWhile {
        there was one(mockSQSClient).retrieveQueuesOfInterest(msg)
        there was one(mockSQSClient).send(queueA, msg)
        there was one(mockSQSClient).send(queueB, msg)
        there was one(mockSQSClient).send(queueC, msg)
      }
    }

    "successfully send a queue message" in new SQSMessageSenderSpecContext {
      val queue = mock[Queue]
      val msg = mock[SQSMessage[_]]

      mockSQSClient.send(any, any) returns Future.unit

      sender ! MsgQueueWithRetry(queue, msg)

      afterAWhile {
        there was one(mockSQSClient).send(queue, msg)
      }
    }

    "retry a failed queue message" in new SQSMessageSenderSpecContext {
      val queue = mock[Queue]
      val msg = mock[SQSMessage[_]]

      mockSQSClient.send(any, any).returns(Future.failed(new RuntimeException), Future.unit)

      sender ! MsgQueueWithRetry(queue, msg)

      afterAWhile {
        there was two(mockSQSClient).send(queue, msg)
      }
    }

    "give up a failed queue message when max tries is reached" in new SQSMessageSenderSpecContext {
      val queue = mock[Queue]
      val msg = mock[SQSMessage[_]]

      mockSQSClient.send(any, any) returns Future.failed(new RuntimeException)

      sender ! MsgQueueWithRetry(queue, msg, tries = testMaxRetries - 1)

      afterAWhile {
        there was one(mockSQSClient).send(queue, msg)
      }
    }

  }
}
