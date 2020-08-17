package io.paytouch.ordering.resources.jetdirect

import java.util.UUID

import akka.http.scaladsl.model._
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.data.model.{ JetdirectConfig => JetdirectConfigModel }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{
  CommonArbitraries,
  FSpec,
  MockedRestApi,
  MultipleLocationFixtures,
  FixtureDaoFactory => Factory,
}

class JetdirectReceiveFSpec extends FSpec with CommonArbitraries {

  abstract class JetdirectReceiveFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val paymentProcessorCallbackDao = daos.paymentProcessorCallbackDao

    def assertCallbackRecorded(status: PaymentProcessorCallbackStatus) = {
      val reference = cleanValues.get("order_number")
      val records = paymentProcessorCallbackDao
        .findByPaymentProcessorAndReferenceAndStatus(PaymentProcessor.Jetdirect, status, reference)
        .await
      records.size ==== 1

      val record = records.head
      record.paymentProcessor ==== PaymentProcessor.Jetdirect
      record.status ==== status
      record.reference ==== reference
      record.payload ==== JsonSupport.fromEntityToJValue(cleanValues)
    }

    def assertCartPaid() = {
      val reloadedCart = daos.cartDao.findById(cart.id).await.get
      reloadedCart.status ==== CartStatus.Paid
      reloadedCart.orderId ==== Some(order.id)
      reloadedCart.orderNumber ==== order.number
    }

    def assertCartUnpaid() = {
      val reloadedCart = daos.cartDao.findById(cart.id).await.get
      reloadedCart.status ==== CartStatus.New
      reloadedCart.orderId ==== None
      reloadedCart.orderNumber ==== None
    }

    val paymentProcessorConfig =
      JetdirectConfigModel(merchantId = "mid", terminalId = "tid", key = "key", securityToken = "sec-token")
    override lazy val merchant = Factory
      .merchant(
        paymentProcessor = Some(PaymentProcessor.Jetdirect),
        paymentProcessorConfig = Some(paymentProcessorConfig),
      )
      .create
    implicit val storeContext = StoreContext.fromRecord(romeStore)

    val order = randomOrder()
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordOrder(order)

    lazy val cartTotalAmount: BigDecimal = 100
    lazy val amount: BigDecimal = 90
    lazy val tipAmount: Option[BigDecimal] = Some(10)
    lazy val cart = Factory.cart(romeStore, totalAmount = Some(100), tipAmount = Some(10)).create
    lazy val maybeCartId = Option(cart.id)
    lazy val responseText = "APPROVED"
    lazy val hashCodeResult =
      MockedRestApi
        .jetDirectService
        .calculateJetdirectReturnHashCode(maybeCartId.map(_.toString).getOrElse(""), amount.toString, responseText)(
          paymentProcessorConfig,
        )

    lazy val formDataValues = Map[String, Option[String]](
      "amount" -> Some(amount.toString),
      "order_number" -> maybeCartId.map(_.toString),
      "responseText" -> Some(responseText),
      "jp_return_hash" -> Some(hashCodeResult),
      "card" -> Some("VS"),
      "tipAmount" -> tipAmount.map(_.toString),
    )
    lazy val cleanValues = formDataValues.flatMap { case (k, v) => v.map(k -> _) }
    lazy val formData = FormData(cleanValues).toEntity
  }

  "POST /v1/vendor/jetdirect/callbacks/receive" in {
    "if it is a valid request" should {
      "if payment is approved" should {
        "respond successfully" in new JetdirectReceiveFSpecContext {
          Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
            assertStatusOK()
            assertNoErrorCode()

            afterAWhile {
              assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
              assertCartPaid()
            }
          }
        }

        "if jetdirect isn't yet sending tip amount" should {
          "respond successfully" in new JetdirectReceiveFSpecContext {
            override lazy val tipAmount = None

            Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
              assertStatusOK()
              assertNoErrorCode()

              afterAWhile {
                assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
                assertCartPaid()
              }
            }
          }
        }
      }

      "if payment is declined" should {
        "respond successfully" in new JetdirectReceiveFSpecContext {
          override lazy val responseText = "DECLINED"

          // card is blank for declined response
          override lazy val formDataValues = Map[String, Option[String]](
            "amount" -> Some(amount.toString),
            "order_number" -> maybeCartId.map(_.toString),
            "responseText" -> Some(responseText),
            "jp_return_hash" -> Some(hashCodeResult),
            "card" -> Some(""),
            "tip_amount" -> tipAmount.map(_.toString),
          )

          Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
            assertStatusOK()
            assertNoErrorCode()

            afterAWhile {
              assertCallbackRecorded(PaymentProcessorCallbackStatus.Declined)
              assertCartUnpaid()
            }
          }
        }
      }
    }

    "if order_number is invalid" should {
      "accept the request with an error" in new JetdirectReceiveFSpecContext {
        override lazy val maybeCartId = Option(UUID.randomUUID)

        Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
          assertStatusOK()
          assertErrorCode("InvalidPaymentProcessorReference")

          afterAWhile {
            assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
            assertCartUnpaid()
          }
        }
      }
    }

    "if order_number is missing" should {
      "accept the request with an error" in new JetdirectReceiveFSpecContext {
        override lazy val maybeCartId: Option[UUID] = None

        Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
          assertStatusOK()
          assertErrorCode("PaymentProcessorMissingMandatoryField")

          afterAWhile {
            assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
            assertCartUnpaid()
          }
        }
      }
    }

    "with error AUTHENTICATEFAIL" should {
      "accept the request with an error" in new JetdirectReceiveFSpecContext {
        override lazy val responseText = "AUTHENTICATEFAIL"

        // card is blank and tip is missing for failure response
        override lazy val formDataValues = Map[String, Option[String]](
          "amount" -> Some(amount.toString),
          "order_number" -> maybeCartId.map(_.toString),
          "responseText" -> Some(responseText),
          "jp_return_hash" -> Some(hashCodeResult),
          "card" -> Some(""),
        )

        Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
          assertStatusOK()
          assertErrorCode("PaymentProcessorUnparsableMandatoryField")

          afterAWhile {
            assertCallbackRecorded(PaymentProcessorCallbackStatus.Failure)
            assertCartUnpaid()
          }
        }
      }
    }

    "if amount is doesn't match cart total" should {
      "accept the request with an error" in new JetdirectReceiveFSpecContext {
        override lazy val amount = genBigDecimal.instance

        Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
          assertStatusOK()
          assertErrorCode("PaymentProcessorTotalMismatch")

          afterAWhile {
            assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
            assertCartUnpaid()
          }
        }
      }
    }

    "if amount is doesn't match cart tip" should {
      "accept the request with an error" in new JetdirectReceiveFSpecContext {
        override lazy val tipAmount = Some(genBigDecimal.instance)

        Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
          assertStatusOK()
          assertErrorCode("PaymentProcessorTipMismatch")

          afterAWhile {
            assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
            assertCartUnpaid()
          }
        }
      }
    }

    "if jp_return_hash doesn't match our expectations" should {
      "accept the request with an error" in new JetdirectReceiveFSpecContext {
        override lazy val hashCodeResult = "I-am-wrong"

        Post(s"/v1/vendor/jetdirect/callbacks/receive", formData) ~> routes ~> check {
          assertStatusOK()
          assertErrorCode("InvalidPaymentProcessorHashCodeResult")

          afterAWhile {
            assertCallbackRecorded(PaymentProcessorCallbackStatus.Success)
            assertCartUnpaid()
          }
        }
      }
    }
  }
}
