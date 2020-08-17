package io.paytouch.ordering.graphql.merchantById

import io.paytouch.ordering.data.model.JetdirectConfig
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class MerchantByIdSpec extends MerchantByIdSchemaSpec {
  val query =
    graphql"""
        query FetchSomeMerchantById($$id: UUID!) {
          merchant: merchantById(id: $$id) {
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
      s"allow to fetch merchant information with config for payment processor $paymentProcessor" in new MerchantByIdSchemaSpecContext {
        override lazy val merchant =
          Factory.merchant(paymentProcessor = Some(paymentProcessor)).create

        val vars = JObject("id" -> JString(merchant.id.toString))
        val result = executeQuery(query, vars = vars)
        val entity =
          merchantService.findById(merchant.id)(userContext).await.get

        result ==== parseAsEntity("merchant", entity)
      }
    }
  }

  "GraphQL Schema" should {
    "allow to fetch data from core" in new MerchantByIdSchemaSpecContext {
      implicit val coreAuthToken =
        ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)
      val merchantEntity = randomMerchant().copy(id = merchant.id)
      PtCoreStubData.recordMerchant(merchantEntity)

      val query =
        graphql"""
        query FetchSomeMerchantById($$id: UUID!) {
          merchant: merchantById(id: $$id) {
            id
            setup_type
          }
        }
        """
      val vars = JObject(
        "id" -> JString(merchant.id.toString),
      )
      val result = executeQuery(query, vars = vars)

      result ==== parseAsEntity("merchant", merchantEntity)
    }
  }
}
