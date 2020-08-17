package io.paytouch.ordering.graphql.merchant

import io.paytouch.ordering.data.model.JetdirectConfig
import io.paytouch.ordering.entities.enums.PaymentProcessor
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class MerchantSpec extends MerchantSchemaSpec {
  val query =
    graphql"""
        query FetchSomeMerchant($$slug: String!) {
          merchant(slug: $$slug) {
            id
            url_slug
            payment_processor
            payment_processor_config {
              ... on EkashuConfig {
                seller_id
                seller_key
              }
              ... on JetdirectConfig {
                terminal_id
                key
              }
              ... on WorldpayConfig {
                account_id
                terminal_id
                acceptor_id
              }
            }
          }
        }
     """

  PaymentProcessor.withPaymentConfig.map { paymentProcessor =>
    "GraphQL Schema" should {
      s"allow to fetch merchant information with config for payment processor $paymentProcessor" in new MerchantSchemaSpecContext {
        override lazy val merchant = Factory.merchant(paymentProcessor = Some(paymentProcessor)).create

        val vars = JObject("slug" -> JString(merchant.urlSlug))
        val result = executeQuery(query, vars = vars)
        val entity = merchantService.findById(merchant.id)(userContext).await.get

        result ==== parseAsEntity("merchant", entity)
      }
    }
  }
}
