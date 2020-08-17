package io.paytouch.ordering.json.serializers

import org.json4s._

import io.paytouch.implicits._

import io.paytouch.ordering.entities._

case object PaymentProcessorConfigSerializer
    extends CustomSerializer[PaymentProcessorConfig](implicit formats =>
      (
        PaymentProcessorConfigSerializerHelper.deserialize(formats),
        {
          {
            // For some reason this doesn't happen automatically and we are forced to do this manually here.
            // TODO: organizing shortHints as we do in core (for both models and entities) should fix this
            case e: EkashuConfig =>
              JObject(JField("seller_id", JString(e.sellerId)) :: JField("seller_key", JString(e.sellerKey)) :: Nil)
            case j: JetdirectConfig =>
              JObject(JField("terminal_id", JString(j.terminalId)) :: JField("key", JString(j.key)) :: Nil)
            case w: WorldpayConfig =>
              JObject(
                JField("account_id", JString(w.accountId)) :: JField("terminal_id", JString(w.terminalId)) :: JField(
                  "acceptor_id",
                  JString(w.acceptorId),
                ) :: Nil,
              )
            case PaytouchConfig | StripeConfig =>
              JObject(Nil)
          }
        },
      ),
    )

object PaymentProcessorConfigSerializerHelper {
  def deserialize(formats: Formats): PartialFunction[JValue, PaymentProcessorConfig] = {
    case JObject(JField("sellerId", JString(sellerId)) :: JField("sellerKey", JString(sellerKey)) :: Nil) =>
      implicit val f: Formats = formats
      EkashuConfig(sellerId, sellerKey)

    case JObject(JField("terminalId", JString(terminalId)) :: JField("key", JString(token)) :: Nil) =>
      implicit val f: Formats = formats
      JetdirectConfig(terminalId, token)

    case JObject(
          JField("accountId", JString(accountId)) :: JField("terminalId", JString(terminalId)) :: JField(
            "acceptorId",
            JString(acceptorId),
          ) :: Nil,
        ) =>
      implicit val f: Formats = formats
      WorldpayConfig(accountId = accountId, terminalId = terminalId, acceptorId = acceptorId)

    case JObject(Nil) => // could be stripe!!!
      PaytouchConfig()
  }
}
