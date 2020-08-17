package io.paytouch.ordering.graphql.store

import java.time.LocalTime
import java.util.UUID

import cats.implicits._

import org.json4s.JsonAST._

import sangria.macros._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.stubs.PtCoreStubData

class StoreSpec extends StoreSchemaSpec {
  trait HasQuery {
    def query =
      graphql"""
         query FetchSomeStore($$merchant_slug: String!, $$slug: String!) {
           store(merchant_slug: $$merchant_slug, slug: $$slug) {
             id
             location_id
             merchant_id
             merchant_url_slug
             url_slug
             catalog_id
             active
             description
             hero_bg_color
             hero_image_urls {
              image_upload_id
              urls {
                original
                thumbnail
                small
                medium
                large
              }
             }
             logo_image_urls {
              image_upload_id
              urls {
                original
                thumbnail
                small
                medium
                large
              }
             }
             take_out_enabled
             take_out_stop_mins_before_closing
             delivery_enabled
             delivery_min { amount, currency }
             delivery_max { amount, currency }
             delivery_max_distance
             delivery_stop_mins_before_closing
             delivery_fee { amount, currency }
             payment_methods {
              type
              active
             }
           }
         }
       """
  }

  "GraphQL Schema" should {
    "allow to fetch store information" in new StoreSchemaSpecContext with HasQuery {
      override lazy val londonHeroImageUrls = Seq(genImageUrls.instance)
      override lazy val londonLogoImageUrls = Seq(genImageUrls.instance)

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)
      val entity = storeService.findById(store.id)(userContext).await.get

      result ==== parseAsEntity("store", entity)
    }

    "allow to fetch opening_hours" in new StoreSchemaSpecContext {
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

      val storeContext = StoreContext.fromRecord(store)
      implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)

      PtCoreStubData.recordCatalog(expectedCatalog)

      def query =
        graphql"""
          query FetchSomeStore($$merchant_slug: String!, $$slug: String!) {
            store(merchant_slug: $$merchant_slug, slug: $$slug) {
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

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(store.urlSlug))
      val result = executeQuery(query, vars = vars)
      val entity = storeService.findById(store.id)(userContext).await.get

      val jsonAsString =
        s"""{
              "data": {
                "store": {
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
  }
}
