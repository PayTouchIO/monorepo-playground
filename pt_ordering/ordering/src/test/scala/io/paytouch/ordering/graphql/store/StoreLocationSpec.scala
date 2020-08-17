package io.paytouch.ordering.graphql.store

import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class StoreLocationSpec extends StoreSchemaSpec {

  abstract class StoreLocationSpecContext extends StoreSchemaSpecContext {
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
  }

  "GraphQL Schema" should {

    "allow to fetch location information of a store" in new StoreLocationSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val expectedLocation =
        random[Location].copy(id = store.locationId)

      PtCoreStubData.recordLocation(expectedLocation)

      val query =
        graphql"""
         query FetchSomeStoreLocation($$merchant_slug: String!, $$slug: String!) {
           store(merchant_slug: $$merchant_slug, slug: $$slug) {
             location {
                id
                name
                email
                phone_number
                website
                active
                address {
                  line1
                  line2
                  city
                  state
                  country
                  state_data {
                    country {
                      code
                      name
                    }
                    code
                    name
                  }
                  postal_code
                }
                timezone
                currency
                coordinates { lat, lng }
             }
           }
         }
       """

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "store" : { "location": $s } }""" }
      result ==== parseAsEntityWithWrapper(wrapper, expectedLocation, fieldsToRemove = Seq("opening_hours", "settings"))
    }
  }
}
