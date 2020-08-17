package io.paytouch.core.async.sqs

import java.util.UUID

import akka.actor.Props
import awscala.s3.Bucket
import awscala.sqs.Message
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.clients.aws.SQSReceiver
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities._

import scala.concurrent._

class SQSMessageConsumerSpec extends SQSSpec with LazyLogging {

  abstract class SQSMessageConsumerSpecContext extends SQSSpecContext {
    @scala.annotation.nowarn("msg=Auto-application")
    implicit val userContext = random[UserContext]
    implicit val merchantContext = userContext.toMerchantContext

    lazy val mockSQSClient = mock[SQSReceiver]

    lazy val mockAction = mock[Bucket] // random mock here
    mockAction.name returns genWord.instance

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
      mockSQSClient.deleteMessage(any) returns Future.unit

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
      mockSQSClient.receiveMessages() returns Future.successful(Seq.empty)

      consumer

      afterAWhile(there was atLeastOne(mockSQSClient).receiveMessages())
    }

    "delete unknown message" in new SQSMessageConsumerSpecContext {
      val mockMessage = mock[Message]
      mockMessage.id returns "message-id"
      mockMessage.body returns """{ "key": "value" }"""

      mockSQSClient.receiveMessages().returns(Future.successful(Seq(mockMessage)), Future.successful(Seq.empty))
      mockSQSClient.deleteMessage(any) returns Future.unit

      consumer

      afterAWhile {
        there was atLeastOne(mockSQSClient).receiveMessages()
        there was noCallsTo(mockAction)
        there was one(mockSQSClient).deleteMessage(mockMessage)
      }
    }

    "process and delete message of type PrepareOrderReceipt" in new SQSMessageConsumerSpecContext {
      val transactionId = UUID.randomUUID

      @scala.annotation.nowarn("msg=Auto-application")
      val order = random[Order]
      val msg = PrepareOrderReceipt(order, Some(transactionId), "email@email.email")
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type LoyaltyMembershipChanged" in new SQSMessageConsumerSpecContext {
      val loyaltyMembership = random[LoyaltyMembership]
      val msg = LoyaltyMembershipChanged(loyaltyMembership)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type GiftCardChanged" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID

      @scala.annotation.nowarn("msg=Auto-application")
      val giftCard = random[GiftCard]
      val msg = GiftCardChanged(merchantId, giftCard)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type GiftCardPassChanged" in new SQSMessageConsumerSpecContext {
      val giftCardPass = random[GiftCardPass]
      val msg = GiftCardPassChanged(giftCardPass)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type OrderSynced" in new SQSMessageConsumerSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val order = random[Order]
      val msg = OrderSynced(order)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type OrderSynced" in new SQSMessageConsumerSpecContext {
      val loyaltyMembership = random[LoyaltyMembership]
      val loyaltyProgram = random[LoyaltyProgram]
      val msg = PrepareLoyaltyMembershipSignedUp(loyaltyMembership, loyaltyProgram)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type ImagesDeleted" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID
      val imageIds = Seq(UUID.randomUUID)
      val msg = ImagesDeleted(imageIds, merchantId)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type ImagesAssociated" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID
      val objectId = UUID.randomUUID
      val imageIds = Seq(UUID.randomUUID)
      val msg = ImagesAssociated(objectId = objectId, imageIds = imageIds, merchantId = merchantId)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type StoresActiveChanged" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID
      val locationItems = random[UpdateActiveItem](3)
      val msg = StoresActiveChanged(merchantId, locationItems)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type RapidoChanged" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID
      val locationItems = random[UpdateActiveItem](3)
      val msg = RapidoChanged(merchantId, locationItems)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and delete message of type PrepareGiftCardPassReceiptRequested" in new SQSMessageConsumerSpecContext {
      val giftCardPass = random[GiftCardPass]
      val msg = PrepareGiftCardPassReceiptRequested(giftCardPass)(userContext)
      assertMessageIsProcessedSuccess(msg)
    }

    "process and not delete message which failed to process" in new SQSMessageConsumerSpecContext {
      val merchantId = UUID.randomUUID
      val locationItems = random[UpdateActiveItem](3)
      val msg = StoresActiveChanged(merchantId, locationItems)
      assertMessageIsProcessedFailure(msg)
    }

  }
}
