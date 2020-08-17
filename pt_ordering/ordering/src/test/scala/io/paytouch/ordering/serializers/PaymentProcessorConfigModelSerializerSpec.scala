package io.paytouch.ordering.serializers

import io.paytouch.ordering.entities._
import io.paytouch.ordering.utils.PaytouchSpec

import org.json4s.jackson.Serialization.{ read, write }
import org.json4s.JsonAST.JString
import org.json4s._

class PaymentProcessorConfigUpsertionSerializerSpec extends PaytouchSpec {
  "PaymentProcessorConfigUpsertionSerializer" should {
    "deserialization from json" should {
      "deserialize ekashu config" in {
        val json = JObject(
          JField("sellerId", JString("sellerId1234")),
          JField("sellerKey", JString("sellerKey1234")),
          JField("hashKey", JString("hashKey1234")),
        )

        val expected = EkashuConfigUpsertion(
          sellerId = "sellerId1234",
          sellerKey = "sellerKey1234",
          hashKey = "hashKey1234",
        )

        json.extract[EkashuConfigUpsertion] ==== expected
        json.extract[PaymentProcessorConfigUpsertion] ==== expected
      }

      "deserialize jetdirect config" in {
        val json = JObject(
          JField("merchantId", JString("merchantId1234")),
          JField("terminalId", JString("terminalId1234")),
          JField("key", JString("key1234")),
          JField("securityToken", JString("securityToken1234")),
        )

        val expected = JetdirectConfigUpsertion(
          merchantId = "merchantId1234",
          terminalId = "terminalId1234",
          key = "key1234",
          securityToken = "securityToken1234",
        )

        json.extract[JetdirectConfigUpsertion] ==== expected
        json.extract[PaymentProcessorConfigUpsertion] ==== expected
      }

      "deserialize worldpay config" in {
        val json = JObject(
          JField("accountId", JString("accountId1234")),
          JField("terminalId", JString("terminalId1234")),
          JField("acceptorId", JString("acceptorId1234")),
          JField("accountToken", JString("accountToken1234")),
        )

        val expected = WorldpayConfigUpsertion(
          accountId = "accountId1234",
          terminalId = "terminalId1234",
          acceptorId = "acceptorId1234",
          accountToken = "accountToken1234",
        )

        json.extract[WorldpayConfigUpsertion] ==== expected
        json.extract[PaymentProcessorConfigUpsertion] ==== expected
      }
    }

    "serialization and deserialization" should {
      "handle ekashu config" in {
        val config = EkashuConfigUpsertion(
          sellerId = "sellerId1234",
          sellerKey = "sellerKey1234",
          hashKey = "hashKey1234",
        )

        read[PaymentProcessorConfigUpsertion](write(config)) ==== config
      }

      "handle jetdirect config" in {
        val config = JetdirectConfigUpsertion(
          merchantId = "merchantId1234",
          terminalId = "terminalId1234",
          key = "key1234",
          securityToken = "securityToken1234",
        )

        read[PaymentProcessorConfigUpsertion](write(config)) ==== config
      }

      "handle worldpay config" in {
        val config = WorldpayConfigUpsertion(
          accountId = "accountId1234",
          terminalId = "terminalId1234",
          acceptorId = "acceptorId1234",
          accountToken = "accountToken1234",
        )

        read[PaymentProcessorConfigUpsertion](write(config)) ==== config
      }
    }
  }
}
