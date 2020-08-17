package io.paytouch.ordering.graphql.catalog

import java.time.LocalTime
import java.util.UUID

import cats.implicits._

import sangria.macros._

import org.json4s.JsonAST._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.UUIDConversion

@scala.annotation.nowarn("msg=Auto-application")
class CatalogSpec extends CatalogSchemaSpec with UUIDConversion {
  "GraphQL Schema" should {
    "allow to fetch catalog information" in new CatalogSchemaSpecContext {
      val locationId: UUID =
        "4a886481-d516-4f4c-b54d-1e534468b22d"

      val expectedCatalog = {
        val expectedLocationOverrides: Map[UUID, CatalogLocation] =
          Map(
            locationId ->
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
         query FetchSomeCatalog($$id: UUID!) {
           catalog(id: $$id) {
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
       """

      val vars = JObject("id" -> JString(catalogId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
              "data": {
                "catalog": {
                  "id": "${expectedCatalog.id}",
                  "name": "${expectedCatalog.name}",
                  "products_count": ${expectedCatalog.productsCount.get},
                  "location_overrides": [{
                    "location_id": "$locationId",
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
            }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }
  }
}
