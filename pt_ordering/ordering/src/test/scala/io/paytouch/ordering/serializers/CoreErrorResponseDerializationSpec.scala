package io.paytouch.ordering.serializers

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core._
import io.paytouch.ordering.utils.PaytouchSpec

import org.json4s.jackson.Serialization.{ read, write }
import org.json4s._
import org.json4s.native.JsonMethods._
import io.paytouch.ordering.clients.paytouch.core.entities.GiftCardPassCharge

class CoreErrorResponseDeserializationSpec extends PaytouchSpec {
  "CoreErrorResponse" should {
    "deserialize auth error" in {
      val json = parse("""
      {
        "errors": [
          "The supplied authentication is invalid"
        ]
      }
      """)

      val err = Extraction.extract[CoreErrorResponse](json)
      err ==== CoreGenericErrorResponse(
        errors = Seq(
          CoreGenericError("The supplied authentication is invalid"),
        ),
      )
    }

    "deserialize out of stock error" in {
      val json = parse("""
      {
        "global": [
        {
          "message": "Product is out of stock",
          "code": "ProductOutOfStock",
          "values": [
          "03d3fcd7-96d1-3868-abee-ab3d0cad7bb4"
          ],
          "object_id": null,
          "field": null
        }
        ],
        "objects_with_errors": {}
      }
      """)

      val err = Extraction.extract[CoreErrorResponse](json)

      err ==== CoreEmbeddedErrorResponse(
        errors = Seq(
          CoreEmbeddedError(
            code = "ProductOutOfStock",
            message = "Product is out of stock",
            values = Seq(
              "03d3fcd7-96d1-3868-abee-ab3d0cad7bb4",
            ),
          ),
        ),
        objectWithErrors = None,
      )
    }

    "deserialize InsufficientFunds" in {
      import io.paytouch._

      val orderId = OrderIdPostgres(UUID.randomUUID()).cast
      val passId1 = GiftCardPass.IdPostgres(UUID.randomUUID()).cast
      val passId2 = GiftCardPass.IdPostgres(UUID.randomUUID()).cast

      val body =
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

      val input =
        s"""|{
            |  "global": [$body],
            |  "objects_with_errors": {}
            |}""".stripMargin

      val expected =
        CoreEmbeddedErrorResponse(
          errors = Seq(
            CoreEmbeddedError(
              code = "InsufficientFunds",
              message =
                s"One or more gift card passes for order: ${orderId.value} did not have sufficient funds to cover the requested amount.",
              values = Seq(
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
            ),
          ),
          objectWithErrors = None,
        )

      val actual = Extraction.extract[CoreErrorResponse](parse(input))

      actual ==== expected
    }
  }
}
