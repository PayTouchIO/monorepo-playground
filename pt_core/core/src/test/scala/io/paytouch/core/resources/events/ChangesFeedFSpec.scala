package io.paytouch.core.resources.events

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ChangesFeedFSpec extends EventsFSpec {

  abstract class ChangesFeedFSpecContext extends BrandResourceFSpecContext {
    val deleted = TrackableAction.Deleted
    val updated = TrackableAction.Updated

    val product = ExposedName.Product
    val category = ExposedName.Category

    val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")
  }

  "GET /v1/changes.feed" in {
    "if request has valid token" should {
      "return a paginated list of brands" in new ChangesFeedFSpecContext {
        val competitorMerchant = Factory.merchant.create

        val event1 = Factory.event(merchant, deleted, product, now.minusDays(2)).await
        val event2 = Factory.event(merchant, deleted, product, now.plusDays(1)).await
        val event3 = Factory.event(merchant, deleted, product, now.plusDays(2)).await

        val event4 = Factory.event(merchant, deleted, category, now.plusDays(3)).await
        val event5 = Factory.event(merchant, updated, product, now.plusDays(4)).await
        val event6 = Factory.event(competitorMerchant, updated, product, now.plusDays(4)).await

        Get(s"/v1/changes.feed?action=${deleted.entryName}&object=${product.entryName}&updated_since=2015-12-03")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val events = responseAs[PaginatedApiResponse[Seq[Event]]].data
          events.map(_.id) ==== Seq(event2.id, event3.id)

          assertResponse(events.find(_.id == event2.id).get, event2)
          assertResponse(events.find(_.id == event3.id).get, event3)
        }
      }
    }
  }

}
