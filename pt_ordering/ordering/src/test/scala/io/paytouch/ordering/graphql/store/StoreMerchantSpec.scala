package io.paytouch.ordering.graphql.store

import io.paytouch.ordering.entities.{ Merchant, StoreContext }
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class StoreMerchantSpec extends StoreSchemaSpec {

  abstract class StoreMerchantSpecContext extends StoreSchemaSpecContext {
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
  }

  "GraphQL Schema" should {

    "allow to fetch merchant information of a store" in new StoreMerchantSpecContext {
      val expectedMerchant = Merchant.fromRecord(merchant)
      val query =
        graphql"""
         query FetchSomeStoreMerchant($$merchant_slug: String!, $$slug: String!) {
           store(merchant_slug: $$merchant_slug, slug: $$slug) {
             merchant {
                id
                url_slug
                payment_processor
                payment_processor_config {
                    ... on EkashuConfig {
                      seller_id
                      seller_key
                    }
                }
             }
           }
         }
       """

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "store" : { "merchant": $s } }""" }
      result ==== parseAsEntityWithWrapper(wrapper, expectedMerchant)
    }
  }
}
