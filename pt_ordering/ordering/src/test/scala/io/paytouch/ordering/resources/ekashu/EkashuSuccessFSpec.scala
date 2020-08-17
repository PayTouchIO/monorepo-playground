package io.paytouch.ordering.resources.ekashu

import java.util.UUID

import akka.http.scaladsl.model._
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class EkashuSuccessFSpec extends FSpec with CommonArbitraries {

  abstract class EkashuSuccessFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val ekashuCallbackDao = daos.paymentProcessorCallbackDao

    def assertCallbackRecorded = {
      val reference = cleanValues.get("ekashu_reference")
      val status = PaymentProcessorCallbackStatus.Success

      val records = ekashuCallbackDao
        .findByPaymentProcessorAndReferenceAndStatus(PaymentProcessor.Ekashu, status, reference)
        .await
      records.size ==== 1

      val record = records.head
      record.paymentProcessor ==== PaymentProcessor.Ekashu
      record.status ==== status
      record.reference ==== reference
      record.payload ==== JsonSupport.fromEntityToJValue(cleanValues)
    }

    implicit val storeContext = StoreContext.fromRecord(romeStore)

    val order = randomOrder()
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordOrder(order)

    lazy val cart = Factory.cart(romeStore).create
    lazy val maybeCartId = Option(cart.id)
    lazy val hashCodeResult = "W7B3Oy+11DvLmFG9hcvi8BZOEWA="
    lazy val ekashuAmount = cart.totalAmount
    lazy val formDataValues = Map[String, Option[String]](
      "ekashu_amount" -> Some(ekashuAmount.toString),
      "ekashu_currency" -> Some("USD"),
      "ekashu_reference" -> maybeCartId.map(_.toString),
      "ekashu_transaction_id" -> Some("banana"),
      "ekashu_hash_code_result" -> Some(hashCodeResult),
    )
    lazy val cleanValues = formDataValues.flatMap { case (k, v) => v.map(k -> _) }
    lazy val formData = FormData(cleanValues).toEntity
  }

  "POST /v1/vendor/ekashu/callbacks/success" in {
    "if it is a valid request" should {
      "respond successfully" in new EkashuSuccessFSpecContext {
        Post(s"/v1/vendor/ekashu/callbacks/success", formData) ~> routes ~> check {
          assertStatusOK()

          val reloadedCart = daos.cartDao.findById(cart.id).await.get
          reloadedCart.orderId === Some(order.id)
          reloadedCart.orderNumber === order.number

          afterAWhile(assertCallbackRecorded)
        }
      }

      "syncs the cart to core" in new EkashuSuccessFSpecContext {
        Post(s"/v1/vendor/ekashu/callbacks/success", formData) ~> routes ~> check {
          assertStatusOK()

          afterAWhile(assertCallbackRecorded)

          val reloadedCart = daos.cartDao.findById(cart.id).await.get
          reloadedCart.status ==== CartStatus.Paid
          reloadedCart.orderId.isDefined ==== true
        }
      }
    }

    "if ekashu_reference is invalid" should {
      "reject the request" in new EkashuSuccessFSpecContext {
        override lazy val maybeCartId = Option(UUID.randomUUID)

        Post(s"/v1/vendor/ekashu/callbacks/success", formData) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("InvalidPaymentProcessorReference")

          afterAWhile(assertCallbackRecorded)
        }
      }

      "reject the request" in new EkashuSuccessFSpecContext {
        override lazy val maybeCartId: Option[UUID] = None

        Post(s"/v1/vendor/ekashu/callbacks/success", formData) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("InvalidPaymentProcessorReference")

          afterAWhile(assertCallbackRecorded)
        }
      }
    }

    "if ekashu_amount is doesn't match cart total" should {
      "reject the request" in new EkashuSuccessFSpecContext {
        override lazy val ekashuAmount = cart.totalAmount - genBigDecimal.instance

        Post(s"/v1/vendor/ekashu/callbacks/success", formData) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("PaymentProcessorTotalMismatch")

          afterAWhile(assertCallbackRecorded)
        }
      }
    }

    "if ekashu_hash_code_result doesn't match our expectations" should {
      "reject the request" in new EkashuSuccessFSpecContext {
        override lazy val hashCodeResult = "I-am-wrong"

        Post(s"/v1/vendor/ekashu/callbacks/success", formData) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("InvalidPaymentProcessorHashCodeResult")

          afterAWhile(assertCallbackRecorded)
        }
      }
    }
  }
}
