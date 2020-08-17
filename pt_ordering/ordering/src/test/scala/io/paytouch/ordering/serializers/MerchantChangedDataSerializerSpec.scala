// package io.paytouch.ordering.serializers

// import io.paytouch.ordering.entities._
// import io.paytouch.ordering.entities.enums.PaymentProcessor
// import io.paytouch.ordering.messages.entities.{ MerchantChanged, MerchantChangedData }
// import io.paytouch.ordering.utils.PaytouchSpec

// import org.json4s.jackson.Serialization.{ read, write }
// import org.json4s.JsonAST.JString
// import org.json4s._

// class MerchantChangedDataSerializerSpec extends PaytouchSpec {
//   "MerchantChangedDataSerializer" should {
//     "it deserializes a complete MerchantChanged message" in {
//       val ast = unmarshalToCamelCase(loadResource("/sqs/merchant_changed_paytouch.json"))

//       val result = fromJsonToEntity[MerchantChanged](ast)
//       result.payload.data === MerchantChangedData(
//         displayName = "Dash Merchant 2",
//         paymentProcessor = None,
//         paymentProcessorConfig = None,
//       )
//     }

//     "deserialization from json" should {
//       "deserialize dummy paytouch config" in {
//         val json = JObject(
//           List(
//             JField("id", JString("43765f06-ce4d-4394-bfb9-91b1ef89fd31")),
//             JField("displayName", JString("Carlo's Coffee")),
//             JField("paymentProcessor", JString("paytouch")),
//             JField(
//               "paymentProcessorConfig",
//               JObject(
//                 JField("jsonClass", JString("PaymentProcessorConfig$PaytouchConfigEntity")),
//               ),
//             ),
//           ),
//         )

//         val expected = MerchantChangedData(
//           displayName = "Carlo's Coffee",
//           paymentProcessor = None,
//           paymentProcessorConfig = None,
//         )

//         json.extract[MerchantChangedData] ==== expected
//       }

//       "deserialize stripe config" in {
//         val json = JObject(
//           JField("id", JString("43765f06-ce4d-4394-bfb9-91b1ef89fd31")),
//           JField("displayName", JString("Carlo's Coffee")),
//           JField("paymentProcessor", JString("stripe")),
//           JField(
//             "paymentProcessorConfig",
//             JObject(
//               JField("accountId", JString("account1234")),
//               JField("publishableKey", JString("key1234")),
//               JField("jsonClass", JString("PaymentProcessorConfig$StripeConfigEntity")),
//             ),
//           ),
//         )

//         val expected = MerchantChangedData(
//           displayName = "Carlo's Coffee",
//           paymentProcessor = Some(PaymentProcessor.Stripe),
//           paymentProcessorConfig = Some(
//             StripeConfigUpsertion(
//               accountId = "account1234",
//               publishableKey = "key1234",
//             ),
//           ),
//         )

//         json.extract[MerchantChangedData] ==== expected
//       }
//     }
//   }
// }
