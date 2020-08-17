package io.paytouch.ordering.resources.worldpay

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, ValidationRejection }
import cats.implicits._
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.data.model.WorldpayPaymentRecord
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.stubs.WorldpayStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.ordering.entities.enums.PaymentIntentStatus
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus

class WorldpayReceiveFSpec extends WorldpayFSpec {
  abstract class WorldpayReceiveFSpecContext extends WorldpayFSpecContext {
    val callbackDao = daos.paymentProcessorCallbackDao

    val payment: WorldpayPaymentRecord
    lazy val transactionSetupId: String = payment.transactionSetupId
    val approvedAmount: BigDecimal
    lazy val queryValues = Map[String, String](
      "status" -> "success",
      "HostedPaymentStatus" -> "Complete",
      "TransactionSetupID" -> transactionSetupId,
      "TransactionID" -> "15532285",
      "ExpressResponseCode" -> "0",
      "ExpressResponseMessage" -> "Approved",
      "AVSResponseCode" -> "N",
      "ApprovalNumber" -> "000050",
      "LastFour" -> "0006",
      "ValidationCode" -> "SDKDS3242323",
      "CardLogo" -> "Visa",
      "ApprovedAmount" -> approvedAmount.toString,
      "Bin" -> "489528",
      "Entry" -> "Manual",
      "TranDT" -> "2019-06-28 09 -> 50 -> 20",
    )

    lazy val query = Uri.Query(queryValues)

    def assertCallbackRecorded(id: UUID, status: PaymentProcessorCallbackStatus) = {
      val reference = Some(id.toString)
      val processor = PaymentProcessor.Worldpay

      val records = callbackDao
        .findByPaymentProcessorAndReferenceAndStatus(processor, status, reference)
        .await
      records.size ==== 1

      val record = records.head
      record.paymentProcessor ==== processor
      record.status ==== status
      record.reference ==== reference
      record.payload ==== JsonSupport.fromEntityToJValue(queryValues)
    }

    def recordQueryResponse() =
      WorldpayStubData.recordQueryResponse(
        transactionSetupId,
        TransactionQueryResponse(
          accountId = "1066410",
          applicationId = "10025",
          approvalNumber = "651831",
          approvedAmount = approvedAmount,
          maskedCardNumber = "************0006",
          cardType = CardType.Visa,
          cardHolderName = "VANTIV TEST EMV 606- PayTouch",
          terminalId = "0060810007",
          transactionId = "19365463",
          transactionSetupId = transactionSetupId,
          hostResponseCode = "00",
        ),
      )
  }

  "POST /v1/vendor/worldpay/callbacks/receive" in {
    "object = cart" should {
      abstract class CartContext extends WorldpayReceiveFSpecContext {
        lazy val cartStatus: CartStatus = CartStatus.New

        lazy val cart = Factory
          .cart(
            romeStore,
            status = Some(cartStatus),
            orderId = order.id.some,
          )
          .create

        lazy val payment = Factory
          .worldpayPayment(
            cart = cart,
            successReturnUrl = Uri("https://order-dev.paytouch.io/eatly/status/success"),
            failureReturnUrl = Uri("https://order-dev.paytouch.io/eatly/status/failure"),
          )
          .create

        lazy val approvedAmount: BigDecimal = cart.totalAmount

        override lazy val acceptanceStatus = AcceptanceStatus.Pending

        def assertCart(cartStatus: CartStatus): Unit = {
          val reloadedCart = daos.cartDao.findById(cart.id).await.get
          reloadedCart.status ==== cartStatus

          // Assert synced to core
          if (reloadedCart.status == CartStatus.Paid)
            reloadedCart.orderId ==== Some(order.id)
        }
      }

      "HostedPaymentStatus = Complete" should {
        "if it is a valid request" should {
          "redirect to success url" in new CartContext {
            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)

              assertWorldpayPayment(transactionSetupId, WorldpayPaymentStatus.Complete)
              assertCart(CartStatus.Paid)

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }

        "if the request is made again" should {
          "redirect to success url" in new CartContext {
            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)
            }

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)
            }
          }
        }

        "if the approved amount doesn't match the cart total" should {
          "reject the request" in new CartContext {
            cart.totalAmount !=== 10.00
            override lazy val approvedAmount: BigDecimal = 10.00

            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("PaymentProcessorTotalMismatch")

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }

        "if the cart is already marked as paid" should {
          "accept the request" in new CartContext {
            override lazy val cartStatus = CartStatus.Paid

            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)

              assertWorldpayPayment(transactionSetupId, WorldpayPaymentStatus.Complete)
              assertCart(CartStatus.Paid)

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }
      }

      "HostedPaymentStatus = Cancelled" should {
        "if it is a valid request" should {
          "redirect to failure url" in new CartContext {
            override lazy val queryValues = Map[String, String](
              "HostedPaymentStatus" -> "Cancelled",
              "TransactionSetupID" -> transactionSetupId,
            )

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.failureReturnUrl)

              assertWorldpayPayment(transactionSetupId, WorldpayPaymentStatus.Cancelled)
              assertCart(CartStatus.New)

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Failure))
            }
          }
        }

        "if the request is made again" should {
          "redirect to failure url" in new CartContext {
            override lazy val queryValues = Map[String, String](
              "HostedPaymentStatus" -> "Cancelled",
              "TransactionSetupID" -> transactionSetupId,
            )

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.failureReturnUrl)
            }

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.failureReturnUrl)
            }
          }
        }
      }

      "if transaction setup id doesn't exist" in {
        "return 404" in new CartContext {
          override lazy val transactionSetupId: String = "1111"

          Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("WorldpayPaymentNotFound")
          }
        }
      }
    }

    "object = payment intent" should {
      abstract class PaymentIntentContext extends WorldpayReceiveFSpecContext {
        lazy val paymentIntentStatus: PaymentIntentStatus = PaymentIntentStatus.New

        lazy val paymentIntent = Factory.paymentIntent(merchant, order.id, status = Some(paymentIntentStatus)).create

        lazy val payment = Factory
          .worldpayPayment(
            paymentIntent = paymentIntent,
            successReturnUrl = Uri("https://order-dev.paytouch.io/eatly/status/success"),
            failureReturnUrl = Uri("https://order-dev.paytouch.io/eatly/status/failure"),
          )
          .create

        lazy val approvedAmount: BigDecimal = paymentIntent.totalAmount

        def assertPaymentIntent(paymentIntentStatus: PaymentIntentStatus): Unit = {
          val reloadedPaymentIntent = daos.paymentIntentDao.findById(paymentIntent.id).await.get
          reloadedPaymentIntent.status ==== paymentIntentStatus
        }
      }

      "HostedPaymentStatus = Complete" should {
        "if it is a valid request" should {
          "redirect to success url" in new PaymentIntentContext {
            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)

              assertWorldpayPayment(transactionSetupId, WorldpayPaymentStatus.Complete)
              assertPaymentIntent(PaymentIntentStatus.Paid)

              afterAWhile(assertCallbackRecorded(paymentIntent.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }

        "if the request is made again" should {
          "redirect to success url" in new PaymentIntentContext {
            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)
            }

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)
            }
          }
        }

        "if the approved amount doesn't match the paymentIntent total" should {
          "reject the request" in new PaymentIntentContext {
            paymentIntent.totalAmount !=== 10.00
            override lazy val approvedAmount: BigDecimal = 10.00

            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("PaymentProcessorTotalMismatch")

              afterAWhile(assertCallbackRecorded(paymentIntent.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }

        "if the payment intent is already marked as paid" should {
          "accept the request" in new PaymentIntentContext {
            override lazy val paymentIntentStatus = PaymentIntentStatus.Paid

            recordQueryResponse()

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.successReturnUrl)

              assertWorldpayPayment(transactionSetupId, WorldpayPaymentStatus.Complete)
              assertPaymentIntent(PaymentIntentStatus.Paid)

              afterAWhile(assertCallbackRecorded(paymentIntent.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }
      }

      "HostedPaymentStatus = Cancelled" should {
        "if it is a valid request" should {
          "redirect to failure url" in new PaymentIntentContext {
            override lazy val queryValues = Map[String, String](
              "HostedPaymentStatus" -> "Cancelled",
              "TransactionSetupID" -> transactionSetupId,
            )

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.failureReturnUrl)

              assertWorldpayPayment(transactionSetupId, WorldpayPaymentStatus.Cancelled)
              assertPaymentIntent(PaymentIntentStatus.New)

              afterAWhile(assertCallbackRecorded(paymentIntent.id, PaymentProcessorCallbackStatus.Failure))
            }
          }
        }

        "if the request is made again" should {
          "redirect to failure url" in new PaymentIntentContext {
            override lazy val queryValues = Map[String, String](
              "HostedPaymentStatus" -> "Cancelled",
              "TransactionSetupID" -> transactionSetupId,
            )

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.failureReturnUrl)
            }

            Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
              assertRedirect(payment.failureReturnUrl)
            }
          }
        }
      }

      "if transaction setup id doesn't exist" in {
        "return 404" in new PaymentIntentContext {
          override lazy val transactionSetupId: String = "1111"

          Get(Uri(s"/v1/vendor/worldpay/callbacks/receive").withQuery(query)) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("WorldpayPaymentNotFound")
          }
        }
      }
    }
  }
}
