package io.paytouch.ordering.resources.stripe

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server._

import cats.implicits._

import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.clients.stripe.StripeClientConfig
import io.paytouch.ordering.data.model.WorldpayPaymentRecord
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.stripe.StripeWebhook
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.ServiceConfigurations
import io.paytouch.ordering.services.StripService
import io.paytouch.ordering.stripe.StripeEncodings
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class StripeWebhookFSpec extends StripeFSpec {
  abstract class StripeWebhookFSpecContext extends StripeFSpecContext with StripeEncodings {
    val callbackDao = daos.paymentProcessorCallbackDao

    val eventId = UUID.randomUUID

    val payload: JValue

    lazy val json: String = compact(render(payload))
    lazy val body = HttpEntity(MediaTypes.`application/json`, json)

    val secret = ServiceConfigurations.stripeClientConfig.webhookSecret
    lazy val signature = calculateSignatureHeader(json, secret)
    lazy val stripeSignatureHeader = RawHeader("Stripe-Signature", signature)

    def assertCallbackRecorded(
        cartId: UUID,
        status: PaymentProcessorCallbackStatus,
        count: Int = 1,
      ) = {
      val reference = cartId.toString.some
      val processor = PaymentProcessor.Stripe

      val records =
        callbackDao
          .findByPaymentProcessorAndReferenceAndStatus(processor, status, reference)
          .await

      records.size ==== count

      val record = records.head
      record.paymentProcessor ==== processor
      record.status ==== status
      record.reference ==== reference

      val payloadEntity = JsonSupport.fromJsonToEntity[StripService.CallbackPayload](record.payload)
      payloadEntity.signature ==== signature
      payloadEntity.body.id ==== eventId.toString
    }
  }

  "POST /v1/vendor/stripe/webhook" in {
    "object = cart" should {
      abstract class CartContext extends StripeWebhookFSpecContext {
        lazy val cartStatus: CartStatus = CartStatus.New

        lazy val cart =
          Factory
            .cart(
              romeStore,
              status = Some(cartStatus),
              orderId = order.id.some,
            )
            .create

        def assertCart(cartStatus: CartStatus): Unit = {
          val reloadedCart = daos.cartDao.findById(cart.id).await.get
          reloadedCart.status ==== cartStatus

          // Assert synced to core
          if (reloadedCart.status == CartStatus.Paid)
            reloadedCart.orderId ==== Some(order.id)
        }
      }

      "type = payment_intent.succeeded" should {
        abstract class PaymentIntentSucceededContext extends CartContext {
          val fixture = loadJsonAst("/stripe/webhooks/payment_intent_succeeded.json")

          val cartId: UUID = cart.id
          val orderId: UUID = order.id
          val approvedAmount: BigInt = (cart.totalAmount * 100).toBigInt

          lazy val metadata =
            JObject(
              List(
                JField("order_id", JString(orderId.toString)),
                JField("cart_id", JString(cartId.toString)),
              ),
            )

          // Override the fixture metadata so it refers to an actual order and cart in the system
          lazy val payload: JValue =
            fixture.mapField {
              case (key @ "id", _) =>
                key -> JString(eventId.toString)

              case (key @ "data", data) =>
                key -> data.mapField {
                  case (key @ "object", paymentIntent) =>
                    key -> paymentIntent.mapField {
                      case (key @ "metadata", _) =>
                        key -> metadata

                      case (key @ "amount", _) =>
                        key -> JInt(approvedAmount)

                      case (key @ "charge", charge) =>
                        key -> charge.mapField {
                          case (key @ "amount", _) =>
                            key -> JInt(approvedAmount)

                          case other =>
                            other
                        }

                      case other => other
                    }

                  case other => other
                }

              case other => other
            }

          override lazy val acceptanceStatus = AcceptanceStatus.Pending
        }

        "if it is a valid request" should {
          "accept the request and mark the cart as paid" in new PaymentIntentSucceededContext {
            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Accepted)

              assertCart(CartStatus.Paid)

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }

        "if the request is made again" should {
          "accept the request but do NOT record the callback" in new PaymentIntentSucceededContext {
            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Accepted)
            }

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Accepted)
            }

            afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Success, count = 1))
          }
        }

        "if the cart id can't be found" should {
          "ignore the request" in new PaymentIntentSucceededContext {
            override val cartId = UUID.randomUUID

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              // Return a success message so Stripe doesn't resend the webhook
              assertStatus(StatusCodes.Accepted)

              assertCart(CartStatus.New)
            }
          }
        }

        "if the approved amount doesn't match the cart total" should {
          "reject the request" in new PaymentIntentSucceededContext {
            override val approvedAmount = 1337

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertErrorCode("PaymentProcessorTotalMismatch")
            }
          }
        }

        "if the signature is invalid" should {
          "reject the request" in new PaymentIntentSucceededContext {
            override val secret = StripeClientConfig.WebhookSecret("another-secret")

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertErrorCode("InvalidPaymentProcessorHashCodeResult")
              assertCart(CartStatus.New)
            }
          }
        }

        "if request cannot be decoded as a webhook" should {
          "reject the request" in new PaymentIntentSucceededContext {
            override lazy val json: String = "{\"id\":\"1234\"}"

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              rejection.isInstanceOf[MalformedRequestContentRejection] ==== true
            }
          }
        }

        "if the cart is already marked as paid" should {
          "accept the request" in new PaymentIntentSucceededContext {
            override lazy val cartStatus = CartStatus.Paid

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Accepted)

              assertCart(CartStatus.Paid)

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Success))
            }
          }
        }
      }

      "type = payment_intent.payment_failed" should {
        abstract class PaymentIntentSucceededContext extends CartContext {
          val fixture = loadJsonAst("/stripe/webhooks/payment_intent_payment_failed.json")

          val cartId: UUID = cart.id
          val orderId: UUID = order.id

          lazy val metadata =
            JObject(List(JField("order_id", JString(orderId.toString)), JField("cart_id", JString(cartId.toString))))

          // Override the fixture metadata so it refers to an actual order and cart in the system
          lazy val payload = fixture mapField {
            case ("id", _) =>
              ("id", JString(eventId.toString))
            case ("data", data) =>
              (
                "data",
                data mapField {
                  case ("object", paymentIntent) =>
                    (
                      "object",
                      paymentIntent mapField {
                        case ("metadata", _) => ("metadata", metadata)
                        case other           => other
                      },
                    )
                  case other => other
                },
              )
            case other => other
          }
        }

        "if it is a valid request" should {
          "accept the request and keep the cart as new" in new PaymentIntentSucceededContext {
            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              assertStatus(StatusCodes.Accepted)

              assertCart(CartStatus.New)

              afterAWhile(assertCallbackRecorded(cart.id, PaymentProcessorCallbackStatus.Declined))
            }
          }
        }

        "if the cart id can't be found" should {
          "ignore the request" in new PaymentIntentSucceededContext {
            override val cartId = UUID.randomUUID

            Post(s"/v1/vendor/stripe/webhook", body).addHeader(stripeSignatureHeader) ~> routes ~> check {
              // Return a success message so Stripe doesn't resend the webhook
              assertStatus(StatusCodes.Accepted)

              assertCart(CartStatus.New)
            }
          }
        }
      }
    }
  }
}
