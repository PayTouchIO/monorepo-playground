package io.paytouch.ordering.json.serializers

import io.paytouch.ordering.entities._
import org.json4s.JsonAST.{ JValue, _ }
import org.json4s.{ CustomSerializer, Formats }

case object PaymentProcessorConfigUpsertionSerializer
    extends CustomSerializer[PaymentProcessorConfigUpsertion](implicit formats =>
      (
        PaymentProcessorConfigUpsertionSerializerHelper.deserialize(formats),
        {
          case e: EkashuConfigUpsertion =>
            JObject(
              JField("sellerId", JString(e.sellerId)) :: JField("sellerKey", JString(e.sellerKey)) :: JField(
                "hashKey",
                JString(e.hashKey),
              ) :: Nil,
            )
          case j: JetdirectConfigUpsertion =>
            JObject(
              JField("merchantId", JString(j.merchantId)) :: JField("terminalId", JString(j.terminalId)) :: JField(
                "key",
                JString(j.key),
              ) :: JField("securityToken", JString(j.securityToken)) :: Nil,
            )
          case w: WorldpayConfigUpsertion =>
            JObject(
              JField("accountId", JString(w.accountId)) :: JField("terminalId", JString(w.terminalId)) :: JField(
                "acceptorId",
                JString(w.acceptorId),
              ) :: JField("accountToken", JString(w.accountToken)) :: Nil,
            )
          case s: StripeConfigUpsertion =>
            JObject(
              JField("publishableKey", JString(s.publishableKey)) :: JField("accountId", JString(s.accountId)) :: Nil,
            )
        },
      ),
    )

object PaymentProcessorConfigUpsertionSerializerHelper {

  def deserialize(formats: Formats): PartialFunction[JValue, PaymentProcessorConfigUpsertion] = {
    case JObject(
          JField("sellerId", JString(sellerId)) :: JField("sellerKey", JString(sellerKey)) :: JField(
            "hashKey",
            JString(hashKey),
          ) :: Nil,
        ) =>
      implicit val f: Formats = formats
      EkashuConfigUpsertion(sellerId, sellerKey, hashKey)
    case JObject(
          JField("merchantId", JString(merchantId)) :: JField("terminalId", JString(terminalId)) :: JField(
            "key",
            JString(key),
          ) :: JField("securityToken", JString(securityToken)) :: Nil,
        ) =>
      implicit val f: Formats = formats
      JetdirectConfigUpsertion(merchantId, terminalId, key, securityToken)
    case JObject(
          JField("accountId", JString(accountId)) :: JField("terminalId", JString(terminalId)) :: JField(
            "acceptorId",
            JString(acceptorId),
          ) :: JField("accountToken", JString(accountToken)) :: Nil,
        ) =>
      implicit val f: Formats = formats
      WorldpayConfigUpsertion(
        accountId = accountId,
        terminalId = terminalId,
        acceptorId = acceptorId,
        accountToken = accountToken,
      )
    case JObject(
          JField("publishableKey", JString(publishableKey)) :: JField("accountId", JString(accountId)) :: Nil,
        ) =>
      implicit val f: Formats = formats
      StripeConfigUpsertion(accountId = accountId, publishableKey = publishableKey)
    case JObject(
          JField("jsonClass", JString(_)) :: JField("accountId", JString(accountId)) :: JField(
            "publishableKey",
            JString(publishableKey),
          ) :: Nil,
        ) =>
      implicit val f: Formats = formats
      StripeConfigUpsertion(accountId = accountId, publishableKey = publishableKey)
  }
}
