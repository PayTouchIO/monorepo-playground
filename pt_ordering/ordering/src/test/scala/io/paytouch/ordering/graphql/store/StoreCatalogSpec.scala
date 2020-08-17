package io.paytouch.ordering.graphql.store

import java.time.LocalTime
import java.util.UUID

import cats.implicits._

import sangria.macros._

import org.json4s.JsonAST._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.stubs.PtCoreStubData

class StoreCatalogSpec extends StoreSchemaSpec {
  abstract class StoreCatalogSpecContext extends StoreSchemaSpecContext {
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
  }

  "GraphQL Schema" should {
    "allow to fetch catalog information of a store" in new StoreCatalogSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val expectedCatalog = {
        val expectedLocationOverrides: Map[UUID, CatalogLocation] =
          Map(
            store.locationId ->
              CatalogLocation(
                Day
                  .values
                  .map(_ -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
                  .toMap,
              ),
          )

        random[Catalog]
          .copy(
            id = catalogId,
            productsCount = 10.some,
            locationOverrides = expectedLocationOverrides.some,
          )
      }

      PtCoreStubData.recordCatalog(expectedCatalog)

      val query =
        graphql"""
         query FetchSomeStoreCatalog($$merchant_slug: String!, $$slug: String!) {
           store(merchant_slug: $$merchant_slug, slug: $$slug) {
             catalog {
                id
                name
                products_count
                location_overrides {
                  location_id
                  catalog_location {
                    availabilities {
                      sunday { start, end }
                      monday { start, end }
                      tuesday { start, end }
                      wednesday { start, end }
                      thursday { start, end }
                      friday { start, end }
                      saturday { start, end }
                    }
                  }
                }
             }
           }
         }
       """

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
              "data": {
                "store": {
                  "catalog": {
                    "id": "${expectedCatalog.id}",
                    "name": "${expectedCatalog.name}",
                    "products_count": ${expectedCatalog.productsCount.get},
                    "location_overrides": [{
                      "location_id": "${store.locationId}",
                      "catalog_location": {
                        "availabilities": {
                          "sunday": [{ "start": "12:00:00", "end": "00:00:00" }],
                          "monday": [{ "start": "12:00:00", "end": "00:00:00" }],
                          "tuesday": [{ "start": "12:00:00", "end": "00:00:00" }],
                          "wednesday": [{ "start": "12:00:00", "end": "00:00:00" }],
                          "thursday": [{ "start": "12:00:00", "end": "00:00:00" }],
                          "friday": [{ "start": "12:00:00", "end": "00:00:00" }],
                          "saturday": [{ "start": "12:00:00", "end": "00:00:00" }]
                        }
                      }
                    }]
                  }
                }
              }
            }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }
  }
}
