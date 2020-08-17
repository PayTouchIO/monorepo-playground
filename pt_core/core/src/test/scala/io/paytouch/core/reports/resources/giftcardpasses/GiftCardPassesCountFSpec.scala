package io.paytouch.core.reports.resources.giftcardpasses

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.reports.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardPassesCountFSpec extends GiftCardPassesFSpec {

  def action = "count"

  class GiftCardPassesCountFSpecContext extends GiftCardPassesFSpecContext

  "GET /v1/reports/gift_card_passes.count" in {

    "with no interval" should {

      "without extra params" in new GiftCardPassesCountFSpecContext {
        Get(s"/v1/reports/gift_card_passes.count?$defaultParamsNoInterval")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(ReportCount(key = None, count = 4))
          result ==== expectedResult
        }
      }

      "when no items are found" in new GiftCardPassesCountFSpecContext {
        Get(s"/v1/reports/gift_card_passes.count?$emptyParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult =
            buildExpectedResultWhenNoInterval(emptyFrom, emptyTo, ReportCount(key = None, count = 0))
          result ==== expectedResult
        }
      }

      "with group by value type" in new GiftCardPassesCountFSpecContext {
        Get(s"/v1/reports/gift_card_passes.count?$defaultParamsNoInterval&group_by=value")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(key = Some("100.00"), count = 1),
            ReportCount(key = Some("50.00"), count = 1),
            ReportCount(key = Some("custom"), count = 2),
          )
          result ==== expectedResult
        }
      }
    }

    "with interval" should {

      "without extra params" in new GiftCardPassesCountFSpecContext {
        Get(s"/v1/reports/gift_card_passes.count?$defaultParamsWithInterval")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 2))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 1))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 1))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with group by value" in new GiftCardPassesCountFSpecContext {
        Get(s"/v1/reports/gift_card_passes.count?$defaultParamsWithInterval&group_by=value")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 0))),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(ReportCount(Some("50.00"), 1), ReportCount(Some("custom"), 1)),
            ),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(Some("custom"), 1))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(Some("100.00"), 1))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }
    }

    "with location_id" should {
      "reurn the correct data" in new GiftCardPassesCountFSpecContext {
        Get(s"/v1/reports/gift_card_passes.count?$defaultParamsWithInterval&group_by=value&location_id=${willow1.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(Some("50.00"), 1))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(Some("custom"), 1))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with a non-accessible location" should {
        "reject the request" in new GiftCardPassesCountFSpecContext {
          val newYork = Factory.location(merchant).create

          Get(
            s"/v1/reports/gift_card_passes.count?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${newYork.id}",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
