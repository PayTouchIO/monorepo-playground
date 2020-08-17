package io.paytouch.ordering.serializers

import java.util.UUID

import cats.data._
import cats.implicits._

import org.json4s._
import org.json4s.JsonAST.JString

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.json.serializers.PaymentProcessorConfigUpsertionSerializerHelper
import io.paytouch.ordering.messages.entities._
import io.paytouch.ordering.utils.PaytouchSpec

class CustomSerializationsSpec extends PaytouchSpec {
  "Custom Serializers" should {
    "serialize a big decimal" in {
      "from string" in {
        JString("1").extract[BigDecimal] ==== BigDecimal(1)
      }

      "from int" in {
        JInt(BigInt(1)).extract[BigDecimal] ==== BigDecimal(1)
      }

      "from double" in {
        JDouble(1.2).extract[BigDecimal] ==== BigDecimal(1.2)
      }

      "from decimal" in {
        JDecimal(1.2).extract[BigDecimal] ==== BigDecimal(1.2)
      }

      "from a monetary amount" in {
        val monetaryAmount = JObject(JField("amount", JString("1")), JField("currency", JString("whatever")))
        monetaryAmount.extract[BigDecimal] ==== BigDecimal(1)
      }
    }

    "serialize a boolean" in {
      "from int" in {
        JInt(BigInt(1)).extract[Boolean] ==== true
        JInt(BigInt(0)).extract[Boolean] ==== false
        JInt(BigInt(-1)).extract[Boolean] ==== false
        JInt(BigInt(-2)).extract[Boolean] ==== false
      }

      "from bool" in {
        JBool(true).extract[Boolean] ==== true
        JBool(false).extract[Boolean] ==== false
      }
    }

    "serialize a boolean with default true" in {
      "from int" in {
        true ==== JInt(BigInt(1)).extract[BooleanTrue]
        false ==== JInt(BigInt(0)).extract[BooleanTrue]
        false ==== JInt(BigInt(-1)).extract[BooleanTrue]
        false ==== JInt(BigInt(-2)).extract[BooleanTrue]
      }

      "from bool" in {
        true ==== JBool(true).extract[BooleanTrue]
        false ==== JBool(false).extract[BooleanTrue]
      }

      "from null" in {
        true ==== JNull.extract[BooleanTrue]
      }

      "from nothing" in {
        true ==== JNothing.extract[BooleanTrue]
      }
    }

    "serialize a boolean with default false" in {
      "from int" in {
        true ==== JInt(BigInt(1)).extract[BooleanFalse]
        false ==== JInt(BigInt(0)).extract[BooleanFalse]
        false ==== JInt(BigInt(-1)).extract[BooleanFalse]
        false ==== JInt(BigInt(-2)).extract[BooleanFalse]
      }

      "from bool" in {
        true ==== JBool(true).extract[BooleanFalse]
        false ==== JBool(false).extract[BooleanFalse]
      }

      "from null" in {
        false ==== JNull.extract[BooleanFalse]
      }

      "from nothing" in {
        false ==== JNothing.extract[BooleanFalse]
      }
    }

    "OrderingPaymentProcessorConfigUpsertion" should {
      "render to specific strings" in {
        "worldpay" in {
          val incoming = """{
            "eventName":"merchant_changed",
            "payload":{
              "object":"merchant",
              "merchantId":"ef9dc646-0c24-4ded-8209-3cea2d4e534b",
              "data":{
                "displayName":"Tester's Tasty's",
                "paymentProcessor":"worldpay"
              },
              "orderingPaymentPaymentProcessorConfigUpsertion":{
                "jsonClass":"MerchantPayload$OrderingPaymentProcessorConfigUpsertion$WorldpayConfigUpsertion",
                "accountId":"accountId",
                "acceptorId":"acceptorId",
                "accountToken":"accountToken",
                "terminalId":"terminalId"
              }
            }
          }"""

          val upsertion =
            MerchantPayload
              .OrderingPaymentProcessorConfigUpsertion
              .WorldpayConfigUpsertion(
                accountId = "accountId",
                acceptorId = "acceptorId",
                accountToken = "accountToken",
                terminalId = "terminalId",
              )
          val expectation = MerchantChanged(
            UUID.fromString("ef9dc646-0c24-4ded-8209-3cea2d4e534b"),
            MerchantChangedData(
              displayName = "Tester's Tasty's",
              paymentProcessor = PaymentProcessor.Worldpay,
            ),
            upsertion.some,
          )
          fromJsonStringToEntity[MerchantChanged](incoming) ==== expectation
        }
        "stripe" in {
          val incoming = """{
            "eventName":"merchant_changed",
            "payload":{
              "object":"merchant",
              "merchantId":"ef9dc646-0c24-4ded-8209-3cea2d4e534b",
              "data":{
                "displayName":"Tester's Tasty's",
                "paymentProcessor":"stripe"
              },
              "orderingPaymentPaymentProcessorConfigUpsertion":{
                "jsonClass":"MerchantPayload$OrderingPaymentProcessorConfigUpsertion$StripeConfigUpsertion",
                "accountId":"accountId",
                "publishableKey":"publishableKey",
              }
            }
          }"""

          val upsertion =
            MerchantPayload
              .OrderingPaymentProcessorConfigUpsertion
              .StripeConfigUpsertion(
                accountId = "accountId",
                publishableKey = "publishableKey",
              )
          val expectation = MerchantChanged(
            UUID.fromString("ef9dc646-0c24-4ded-8209-3cea2d4e534b"),
            MerchantChangedData(
              displayName = "Tester's Tasty's",
              paymentProcessor = PaymentProcessor.Stripe,
            ),
            upsertion.some,
          )
          fromJsonStringToEntity[MerchantChanged](incoming) ==== expectation
        }
        "paytouch" in {
          val incoming = """{
            "eventName":"merchant_changed",
            "payload":{
              "object":"merchant",
              "merchantId":"ef9dc646-0c24-4ded-8209-3cea2d4e534b",
              "data":{
                "displayName":"Tester's Tasty's",
                "paymentProcessor":"paytouch"
              },
              "orderingPaymentPaymentProcessorConfigUpsertion":null
            }
          }"""

          val upsertion =
            MerchantPayload
              .OrderingPaymentProcessorConfigUpsertion
              .StripeConfigUpsertion(
                accountId = "accountId",
                publishableKey = "publishableKey",
              )
          val expectation = MerchantChanged(
            UUID.fromString("ef9dc646-0c24-4ded-8209-3cea2d4e534b"),
            MerchantChangedData(
              displayName = "Tester's Tasty's",
              paymentProcessor = PaymentProcessor.Paytouch,
            ),
            None,
          )
          fromJsonStringToEntity[MerchantChanged](incoming) ==== expectation
        }
      }
    }
  }
}
