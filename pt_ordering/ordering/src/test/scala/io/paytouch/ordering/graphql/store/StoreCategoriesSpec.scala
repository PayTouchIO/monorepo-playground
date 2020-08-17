package io.paytouch.ordering.graphql.store

import java.util.UUID

import sangria.macros._

import org.json4s.JsonAST._

import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.stubs.PtCoreStubData

class StoreCategoriesSpec extends StoreSchemaSpec {
  abstract class StoreCategoriesSpecContext extends StoreSchemaSpecContext {
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
  }

  "GraphQL Schema" should {
    "allow to fetch catalog categories information of a store with a catalog id" in new StoreCategoriesSpecContext {
      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeStoreCatalogCategory($$merchant_slug: String!, $$slug: String!) {
           store(merchant_slug: $$merchant_slug, slug: $$slug) {
              categories {
                id
                name
                catalog_id
                merchant_id
                description
                avatar_bg_color
                position
                active
              }
           }
         }
       """

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "store" : { "categories": $s } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        Seq(expectedCategory),
        fieldsToRemove = Seq("avatar_image_urls", "subcategories", "location_overrides", "availabilities"),
      )
    }

    "allow to fetch system categories information of a store with a null catalog id" in new StoreCategoriesSpecContext {
      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeStoreCatalogCategory($$merchant_slug: String!, $$slug: String!) {
           store(merchant_slug: $$merchant_slug, slug: $$slug) {
              categories {
                id
                name
                catalog_id
                merchant_id
                description
                avatar_bg_color
                position
                active
              }
           }
         }
       """

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "store" : { "categories": $s } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        Seq(expectedCategory),
        fieldsToRemove = Seq("avatar_image_urls", "subcategories", "location_overrides", "availabilities"),
      )
    }
  }
}
