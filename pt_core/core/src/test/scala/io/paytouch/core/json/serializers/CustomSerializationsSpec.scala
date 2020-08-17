package io.paytouch.core.json.serializers

import java.time.LocalTime
import java.util.UUID

import org.json4s._
import org.json4s.native.Serialization

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.errors.InsufficientFunds
import io.paytouch.core.messages.entities._
import io.paytouch.core.services._
import io.paytouch.core.services.GiftCardPassService._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class CustomSerializationsSpec extends FSpec {
  "Custom Serializers" should {
    "deserialize correctly a trait-like class" in {
      val paymentDetails = random[PaymentDetails]

      @scala.annotation.nowarn("msg=Auto-application")
      val paymentTransaction = random[PaymentTransaction].copy(paymentDetails = Some(paymentDetails), paidAt = None)

      @scala.annotation.nowarn("msg=Auto-application")
      val order = random[Order].copy(paymentTransactions = Some(Seq(paymentTransaction)))

      val json = fromEntityToJValue(order)
      val parsedOrder = json.extract[Order]

      parsedOrder.paymentTransactions ==== order.paymentTransactions
    }

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

      "from string" in {
        JString("true").extract[Boolean] ==== true
        JString("false").extract[Boolean] ==== false
      }
    }

    "serialize a map with enum as key" in {
      type MyMap = Map[ImageUploadType, Seq[String]]

      Serialization.read[MyMap]("""{ "store_hero": [] }""") ==== Map(ImageUploadType.StoreHero -> Seq.empty[String])
      Serialization.read[MyMap]("""{ "storeHero": [] }""") ==== Map(ImageUploadType.StoreHero -> Seq.empty[String])
    }
  }

  "CommentCreation" should {
    "objectType is ignored" in {
      val id = UUID.randomUUID
      val body = "bla"
      val data = JObject(JField("objectId", JString(id.toString)), JField("body", JString(body)))
      val result = data.extract[CommentCreation]
      result ==== CommentCreation(objectId = id, body = body, objectType = None)
    }
  }

  "LocalDate de/serialization" should {
    "return the same representation for LocalTime" in new FSpecContext with LocalDateAssertions {
      val merchant = Factory.merchant.create
      val location = Factory.location(merchant).create
      val user = Factory.user(merchant).create

      val localTime = LocalTime.parse("23:59:59.999")
      println(s"[flaky][CustomSerializationsSpec debugging] ${localTime.toNanoOfDay}")
      val shiftWrapper = Factory.shift(user, location, startTime = Some(localTime))
      val shift = shiftWrapper.create

      shiftWrapper.update.startTime ==== Some(localTime)
      shift.startTime must beApproxTheSame(localTime)
    }
  }

  "OrderingPaymentProcessorConfigUpsertion" should {
    "render to specific strings" in {
      "worldpay" in {
        val worldpay = MerchantPayload
          .OrderingPaymentProcessorConfigUpsertion
          .WorldpayConfigUpsertion(
            accountId = "accountId",
            acceptorId = "acceptorId",
            accountToken = "accountToken",
            terminalId = "terminalId",
          )

        val expectation =
          """{
            |  "jsonClass":"MerchantPayload$OrderingPaymentProcessorConfigUpsertion$WorldpayConfigUpsertion",
            |  "accountId":"accountId",
            |  "acceptorId":"acceptorId",
            |  "accountToken":"accountToken",
            |  "terminalId":"terminalId"
            |}""".stripMargin

        fromJsonToString(fromEntityToJValue(worldpay)) ==== expectation
      }

      "stripe" in {
        val stripe = MerchantPayload
          .OrderingPaymentProcessorConfigUpsertion
          .StripeConfigUpsertion(
            accountId = "accountId",
            publishableKey = "publishableKey",
          )

        val expectation =
          """{
            |  "jsonClass":"MerchantPayload$OrderingPaymentProcessorConfigUpsertion$StripeConfigUpsertion",
            |  "accountId":"accountId",
            |  "publishableKey":"publishableKey"
            |}""".stripMargin

        fromJsonToString(fromEntityToJValue(stripe)) ==== expectation
      }
    }
  }

  "InsufficientFunds" should {
    "be converted to json" in {
      import io.paytouch._

      val orderId = OrderIdPostgres(UUID.randomUUID()).cast
      val passId1 = GiftCardPass.IdPostgres(UUID.randomUUID()).cast
      val passId2 = GiftCardPass.IdPostgres(UUID.randomUUID()).cast

      val input =
        InsufficientFunds(
          orderId,
          Seq(
            GiftCardPassCharge.Failure(
              giftCardPassId = passId1,
              requestedAmount = 23.1234,
              actualBalance = 22.789,
            ),
            GiftCardPassCharge.Failure(
              giftCardPassId = passId2,
              requestedAmount = 5,
              actualBalance = 0,
            ),
          ),
        )

      val expected =
        s"""|{
            |  "message":"One or more gift card passes for order: ${orderId.value} did not have sufficient funds to cover the requested amount.",
            |  "code":"InsufficientFunds",
            |  "values":[{
            |    "jsonClass":"GiftCardPassCharge$$Failure",
            |    "giftCardPassId":"${passId1.value}",
            |    "requestedAmount":"23.1234",
            |    "actualBalance":"22.789"
            |  },{
            |    "jsonClass":"GiftCardPassCharge$$Failure",
            |    "giftCardPassId":"${passId2.value}",
            |    "requestedAmount":"5",
            |    "actualBalance":"0"
            |  }],
            |  "objectId":null,
            |  "field":null
            |}""".stripMargin

      fromJsonToString(fromEntityToJValue(input)) ==== expected
    }
  }
}
