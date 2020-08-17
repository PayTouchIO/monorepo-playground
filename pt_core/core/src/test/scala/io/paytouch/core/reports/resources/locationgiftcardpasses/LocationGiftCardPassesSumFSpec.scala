package io.paytouch.core.reports.resources.locationgiftcardpasses

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.reports.entities.{ ReportFields, ReportTimeframe, _ }

class LocationGiftCardPassesSumFSpec extends LocationGiftCardPassesFSpec {

  def action = "sum"

  import fixtures._

  "GET /v1/reports/location_gift_card_passes.sum" in {
    "with no interval" should {
      "with no group by" should {
        assertNoField()

        assertFieldResult(
          "customers",
          orderedResult(
            londonData = londonEmptyAggregate.copy(customers = londonFullAggregate.customers),
            romeData = romeEmptyAggregate.copy(customers = romeFullAggregate.customers),
            willow1Data = willow1EmptyAggregate.copy(customers = willow1FullAggregate.customers),
            willow2Data = willow2EmptyAggregate.copy(customers = willow2FullAggregate.customers),
            willow3Data = willow3EmptyAggregate.copy(customers = willow3FullAggregate.customers),
          ): _*,
        )

        assertFieldResult(
          "total",
          orderedResult(
            londonData = londonEmptyAggregate.copy(total = londonFullAggregate.total),
            romeData = romeEmptyAggregate.copy(total = romeFullAggregate.total),
            willow1Data = willow1EmptyAggregate.copy(total = willow1FullAggregate.total),
            willow2Data = willow2EmptyAggregate.copy(total = willow2FullAggregate.total),
            willow3Data = willow3EmptyAggregate.copy(total = willow3FullAggregate.total),
          ): _*,
        )

        assertFieldResult(
          "redeemed",
          orderedResult(
            londonData = londonEmptyAggregate.copy(redeemed = londonFullAggregate.redeemed),
            romeData = romeEmptyAggregate.copy(redeemed = romeFullAggregate.redeemed),
            willow1Data = willow1EmptyAggregate.copy(redeemed = willow1FullAggregate.redeemed),
            willow2Data = willow2EmptyAggregate.copy(redeemed = willow2FullAggregate.redeemed),
            willow3Data = willow3EmptyAggregate.copy(redeemed = willow3FullAggregate.redeemed),
          ): _*,
        )

        assertFieldResult(
          "unused",
          orderedResult(
            londonData = londonEmptyAggregate.copy(unused = londonFullAggregate.unused),
            romeData = romeEmptyAggregate.copy(unused = romeFullAggregate.unused),
            willow1Data = willow1EmptyAggregate.copy(unused = willow1FullAggregate.unused),
            willow2Data = willow2EmptyAggregate.copy(unused = willow2FullAggregate.unused),
            willow3Data = willow3EmptyAggregate.copy(unused = willow3FullAggregate.unused),
          ): _*,
        )

        assertAllFieldsResult(
          orderedResult(
            londonData = londonFullAggregate,
            romeData = romeFullAggregate,
            willow1Data = willow1FullAggregate,
            willow2Data = willow2FullAggregate,
            willow3Data = willow3FullAggregate,
          ): _*,
        )
      }
    }

    "with interval" should {

      "with no group by" should {

        "with no fields it should reject the request" in {
          Get(s"/v1/reports/location_gift_card_passes.sum?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with all fields" in new GiftCardPassesFSpecContext {
          Get(s"/v1/reports/location_gift_card_passes.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[LocationGiftCardPasses]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                orderedResult(
                  londonData = londonW0FullAggregate,
                  romeData = romeW0FullAggregate,
                  willow1Data = willow1W0FullAggregate,
                  willow2Data = willow2W0FullAggregate,
                  willow3Data = willow3W0FullAggregate,
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                orderedResult(
                  londonData = londonW1FullAggregate,
                  romeData = romeW1FullAggregate,
                  willow1Data = willow1W1FullAggregate,
                  willow2Data = willow2W1FullAggregate,
                  willow3Data = willow3W1FullAggregate,
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                orderedResult(
                  londonData = londonW2FullAggregate,
                  romeData = romeW2FullAggregate,
                  willow1Data = willow1W2FullAggregate,
                  willow2Data = willow2W2FullAggregate,
                  willow3Data = willow3W2FullAggregate,
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                orderedResult(
                  londonData = londonW3FullAggregate,
                  romeData = romeW3FullAggregate,
                  willow1Data = willow1W3FullAggregate,
                  willow2Data = willow2W3FullAggregate,
                  willow3Data = willow3W3FullAggregate,
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                orderedResult(
                  londonData = londonW4FullAggregate,
                  romeData = romeW4FullAggregate,
                  willow1Data = willow1W4FullAggregate,
                  willow2Data = willow2W4FullAggregate,
                  willow3Data = willow3W4FullAggregate,
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location" should {
      "return the correct data" in new GiftCardPassesFSpecContext {
        Get(
          s"/v1/reports/location_gift_card_passes.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${london.id}",
        ).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportFields[LocationGiftCardPasses]]]

          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              Seq(londonResult(londonW0FullAggregate)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              Seq(londonResult(londonW1FullAggregate)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(14), end.plusDays(14)),
              Seq(londonResult(londonW2FullAggregate)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(21), end.plusDays(21)),
              Seq(londonResult(londonW3FullAggregate)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(28), end.plusDays(28)),
              Seq(londonResult(londonW4FullAggregate)),
            ),
          )
          result ==== expectedResult
        }
      }

    }
  }
}
