package io.paytouch.ordering.graphql.location

import java.time.LocalTime

import org.json4s.JsonAST.{ JObject, JString }

import sangria.macros._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.stubs.PtCoreStubData

@scala.annotation.nowarn("msg=Auto-application")
class LocationSpec extends LocationSchemaSpec {
  "GraphQL Schema" should {
    "allow to fetch location information" in new LocationSchemaSpecContext {
      val expectedLocation =
        random[Location].copy(id = locationId, openingHours = None, coordinates = Some(Coordinates(1, 2)))
      PtCoreStubData.recordLocation(expectedLocation)

      val query =
        graphql"""
         query FetchSomeLocation($$id: UUID!) {
           location(id: $$id) {
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
       """

      val vars = JObject("id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "location": $s }""" }
      result ==== parseAsEntityWithWrapper(wrapper, expectedLocation, fieldsToRemove = Seq("opening_hours", "settings"))
    }

    "allow to fetch location opening hours information" in new LocationSchemaSpecContext {
      val openingHours: Map[Day, Seq[Availability]] = Day
        .values
        .map(day => day -> Seq(Availability(start = LocalTime.NOON, end = LocalTime.MIDNIGHT)))
        .toMap

      val location = random[Location].copy(id = locationId, openingHours = Some(openingHours))
      PtCoreStubData.recordLocation(location)

      val query =
        graphql"""
         query FetchSomeLocation($$id: UUID!) {
           location(id: $$id) {
              opening_hours {
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

      val vars = JObject("id" -> JString(locationId.toString))
      val result = executeQuery(query, vars = vars)

      val jsonAsString =
        """{
            "data": {
              "location": {
                "opening_hours": {
                  "sunday": [{ "start": "12:00:00", "end": "00:00:00" }],
                  "monday": [{ "start": "12:00:00", "end": "00:00:00" }],
                  "tuesday": [{ "start": "12:00:00", "end": "00:00:00" }],
                  "wednesday": [{ "start": "12:00:00", "end": "00:00:00" }],
                  "thursday": [{ "start": "12:00:00", "end": "00:00:00" }],
                  "friday": [{ "start": "12:00:00", "end": "00:00:00" }],
                  "saturday": [{ "start": "12:00:00", "end": "00:00:00" }]
                }
              }
            }
        }""".stripMargin

      result ==== parseAsSnakeCase(jsonAsString)
    }

    "default_estimated_prep_time_in_mins" should {
      "allow to fetch default_estimated_prep_time_in_mins" in new LocationSchemaSpecContext {
        val settings = LocationSettings(
          onlineOrder = LocationOnlineOrderSettings(
            defaultEstimatedPrepTimeInMins = Some(25),
          ),
        )

        val location = random[Location].copy(id = locationId, settings = Some(settings))
        PtCoreStubData.recordLocation(location)

        val query =
          graphql"""
          query FetchSomeLocation($$id: UUID!) {
            location(id: $$id) {
                default_estimated_prep_time_in_mins
            }
          }
        """

        val vars = JObject("id" -> JString(locationId.toString))
        val result = executeQuery(query, vars = vars)

        val jsonAsString =
          """{
              "data": {
                "location": {
                  "default_estimated_prep_time_in_mins": 25
                }
              }
          }""".stripMargin

        result ==== parseAsSnakeCase(jsonAsString)
      }

      "returns null if location settings are not available" in new LocationSchemaSpecContext {
        val location = random[Location].copy(id = locationId, settings = None)
        PtCoreStubData.recordLocation(location)

        val query =
          graphql"""
          query FetchSomeLocation($$id: UUID!) {
            location(id: $$id) {
                default_estimated_prep_time_in_mins
            }
          }
        """

        val vars = JObject("id" -> JString(locationId.toString))
        val result = executeQuery(query, vars = vars)

        val jsonAsString =
          """{
              "data": {
                "location": {
                  "default_estimated_prep_time_in_mins": null
                }
              }
          }""".stripMargin

        result ==== parseAsSnakeCase(jsonAsString)
      }
    }
  }
}
