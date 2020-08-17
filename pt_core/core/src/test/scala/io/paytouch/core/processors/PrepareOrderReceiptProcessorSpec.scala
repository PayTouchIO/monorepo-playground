package io.paytouch.core.processors

import java.util.UUID

import scala.concurrent._
import scala.concurrent.duration._

import com.softwaremill.macwire._

import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ MerchantExpansions, OrderExpansions }
import io.paytouch.core.messages.entities.{ OrderReceiptRequestedV2, PrepareOrderReceipt }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.{ LoyaltyMembershipService, LoyaltyProgramService }
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class PrepareOrderReceiptProcessorSpec extends ProcessorSpec {
  private val timeout = 4.minutes

  abstract class PrepareOrderReceiptProcessorSpecContext extends ProcessorSpecContext with MultipleLocationFixtures {
    implicit val u: UserContext = userContext

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val loyaltyMembershipService = mock[LoyaltyMembershipService]
    val loyaltyProgramService = mock[LoyaltyProgramService]
    val merchantService = MockedRestApi.merchantService
    val orderService = MockedRestApi.orderService
    val locationEmailReceiptService = MockedRestApi.locationEmailReceiptService
    val locationReceiptService = MockedRestApi.locationReceiptService

    val processor = wire[PrepareOrderReceiptProcessor]

    val globalCustomer = Factory.globalCustomer().create
    val customer = Factory.customerMerchant(merchant, globalCustomer).create
    val order = Factory.order(merchant, location = Some(rome), customer = Some(customer)).create
    Factory.locationEmailReceipt(rome).create
    Factory.locationReceipt(rome).create

    val recipientEmail = "foo@bar.it"

    val orderEntity = orderService.enrich(order, orderService.defaultFilters)(OrderExpansions.withFullOrderItems).await
    val merchantEntity = merchantService.findById(merchant.id)(MerchantExpansions.none).await.get
    val locationEmailReceiptEntity = locationEmailReceiptService.findByLocationId(rome.id).await.get
    val locationReceiptEntity = locationReceiptService.findByLocationId(rome.id).await.get

    def assertNoLoyaltyMembershipCreated() =
      there was no(loyaltyMembershipService).findOrCreateInActiveProgram(any, any)(any)

    def assertLoyaltyMembershipCreated(customerId: UUID, locationId: UUID) =
      there was one(loyaltyMembershipService).findOrCreateInActiveProgram(customerId, Some(locationId))

    def assertOrderReceiptRequestedMessageV2Enqueued(
        order: Order,
        paymentTransactionId: Option[UUID] = None,
        loyaltyMembership: Option[LoyaltyMembership] = None,
        loyaltyProgram: Option[LoyaltyProgram] = None,
      ) = {
      val expectedMsg = SendMsgWithRetry(
        OrderReceiptRequestedV2(
          order,
          paymentTransactionId,
          recipientEmail,
          merchantEntity,
          locationReceiptEntity,
          loyaltyMembership,
          loyaltyProgram,
        ),
      )
      actorMock.expectMsg(timeout, expectedMsg)
      ok
    }
  }

  "PrepareOrderReceiptProcessor" in {
    "execute" in {
      "if order entity does not contain customer" should {
        "send receipt" in new PrepareOrderReceiptProcessorSpecContext {
          val orderEntityNoCustomer = orderEntity.copy(customer = None)
          val prepareOrderReceiptMessage = PrepareOrderReceipt(orderEntityNoCustomer, None, recipientEmail)
          val expectedLoyaltyProgram = random[LoyaltyProgram]
          loyaltyMembershipService.updateLinksWithOrderId(None, orderEntity.id) returns None
          loyaltyProgramService.findByOptId(None) returns Future.successful(Some(expectedLoyaltyProgram))

          processor.execute(prepareOrderReceiptMessage)

          afterAWhile {
            assertNoLoyaltyMembershipCreated()
          }
          assertOrderReceiptRequestedMessageV2Enqueued(
            orderEntityNoCustomer,
            loyaltyMembership = None,
            loyaltyProgram = Some(expectedLoyaltyProgram),
          )
        }
      }
      "if order entity does contains customer" should {
        "link customer to loyalty program and send receipt" in new PrepareOrderReceiptProcessorSpecContext {
          val expectedLoyaltyMembership = random[LoyaltyMembership]
          val expectedLoyaltyProgram = random[LoyaltyProgram]

          loyaltyMembershipService.findOrCreateInActiveProgram(customer.id, Some(rome.id)) returns
            Future.successful(Some(expectedLoyaltyMembership))
          loyaltyMembershipService.updateLinksWithOrderId(Some(expectedLoyaltyMembership), orderEntity.id) returns
            Some(expectedLoyaltyMembership)
          loyaltyProgramService.findByOptId(Some(expectedLoyaltyMembership.loyaltyProgramId)) returns
            Future.successful(Some(expectedLoyaltyProgram))

          val prepareOrderReceiptMessage = PrepareOrderReceipt(orderEntity, None, recipientEmail)

          processor.execute(prepareOrderReceiptMessage)

          afterAWhile {
            assertLoyaltyMembershipCreated(customer.id, rome.id)
          }
          assertOrderReceiptRequestedMessageV2Enqueued(
            orderEntity,
            loyaltyMembership = Some(expectedLoyaltyMembership),
            loyaltyProgram = Some(expectedLoyaltyProgram),
          )
        }
      }
    }
  }
}
