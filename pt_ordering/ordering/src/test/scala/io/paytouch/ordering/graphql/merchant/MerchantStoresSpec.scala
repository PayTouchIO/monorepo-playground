package io.paytouch.ordering.graphql.merchant

import sangria.macros._

import org.json4s.JsonAST._

import io.paytouch.ordering.entities.Merchant

class MerchantStoresSpec extends MerchantSchemaSpec {
  abstract class MerchantStoreSpecContext extends MerchantSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)
    val expectedStores = stores
  }

  "GraphQL Schema" should {
    "allow to fetch stores information of a merchant" in new MerchantStoreSpecContext {
      val query =
        graphql"""
         query FetchMerchantStores($$slug: String!) {
           merchant(slug: $$slug) {
             stores {
                id
             }
           }
         }
       """

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(merchant.urlSlug))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "stores": $s } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        expectedStores.map(s => "id" -> s.id),
        fieldsToRemove = Seq("opening_hours"),
      )
    }
  }
}
