package io.paytouch.ordering.graphql.categories

import java.time.LocalTime
import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ Day, ImageSize }
import io.paytouch.ordering.clients.paytouch.core.entities.{ Availability, Category, CategoryLocation, ImageUrls }
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JNull, JObject, JString }
import sangria.macros._

class CategoriesSpec extends CategoriesSchemaSpec {
  "GraphQL Schema" should {
    "allow to fetch catalog categories information" in new CategoriesSchemaSpecContext {
      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
           categories(catalog_id: $$catalog_id,location_id: $$location_id) {
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
       """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "categories": $s }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        Seq(expectedCategory),
        fieldsToRemove = Seq("avatar_image_urls", "subcategories", "location_overrides", "availabilities"),
      )
    }

    "allow to fetch categories avatar images" in new CategoriesSchemaSpecContext {
      override lazy val expectedImageUrls = Seq(
        ImageUrls(
          imageUploadId = UUID.fromString("c7987cca-6463-453d-9806-1940a3aa4d6e"),
          urls = ImageSize.values.map(value => value -> s"my-url-${value.entryName}").toMap,
        ),
      )
      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
           categories(catalog_id: $$catalog_id, location_id: $$location_id) {
              avatar_image_urls {
                image_upload_id
                urls {
                  original
                  thumbnail
                  small
                  medium
                  large
                }
              }
           }
         }
       """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        """{
          	"data": {
          		"categories": [{
          			"avatar_image_urls": [{
          				"image_upload_id": "c7987cca-6463-453d-9806-1940a3aa4d6e",
          				"urls": {
          					"original": "my-url-original",
          					"thumbnail": "my-url-thumbnail",
          					"small": "my-url-small",
          					"medium": "my-url-medium",
          					"large": "my-url-large"
          				}
          			}]
          		}]
          	}
          }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories subcategories" in new CategoriesSchemaSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      override lazy val expectedSubcategories =
        random[Category](2)

      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
           categories(catalog_id: $$catalog_id, location_id: $$location_id) {
              subcategories {
                id
              }
           }
         }
       """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
          	"data": {
          		"categories": [{
          			"subcategories": [{
          				"id": "${expectedSubcategories.head.id}"
          			},
          			{
                  "id": "${expectedSubcategories(1).id}"
                }]
          		}]
          	}
          }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories location overrides" in new CategoriesSchemaSpecContext {
      val expectedLocationAvailabilities: Map[Day, Seq[Availability]] = Day
        .values
        .map(day => day -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
        .toMap
      override lazy val expectedLocationOverrides =
        Map(locationId -> CategoryLocation(active = true, availabilities = expectedLocationAvailabilities))

      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
           categories(catalog_id: $$catalog_id, location_id: $$location_id) {
              location_overrides {
                location_id
                category_location {
                  active
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

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
              "data": {
                "categories": [{
                  "location_overrides": [{
                    "location_id": "$locationId",
                    "category_location": {
                      "active": true,
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
                }]
              }
            }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "allow to fetch categories availabilities" in new CategoriesSchemaSpecContext {
      override lazy val expectedAvailabilities: Map[Day, Seq[Availability]] = Day
        .values
        .map(day => day -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
        .toMap
      PtCoreStubData.recordCategory(expectedCategory)

      val query =
        graphql"""
         query FetchSomeCategory($$catalog_id: UUID!, $$location_id: UUID!) {
           categories(catalog_id: $$catalog_id, location_id: $$location_id) {
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
       """

      val vars = JObject("catalog_id" -> JString(catalogId.toString), "location_id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        s"""{
              "data": {
                "categories": [{
                  "availabilities": {
                    "sunday": [{ "start": "12:00:00", "end": "00:00:00" }],
                    "monday": [{ "start": "12:00:00", "end": "00:00:00" }],
                    "tuesday": [{ "start": "12:00:00", "end": "00:00:00" }],
                    "wednesday": [{ "start": "12:00:00", "end": "00:00:00" }],
                    "thursday": [{ "start": "12:00:00", "end": "00:00:00" }],
                    "friday": [{ "start": "12:00:00", "end": "00:00:00" }],
                    "saturday": [{ "start": "12:00:00", "end": "00:00:00" }]
                  }
                }]
              }
            }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }
  }
}
