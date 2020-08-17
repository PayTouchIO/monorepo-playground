package io.paytouch.core.reports.resources.orders

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.{ ReportFields, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class OrdersSumFSpec extends OrdersFSpec {
  def action = "sum"

  import fixtures._

  class OrdersSumFSpecContext extends OrdersFSpecContext

  "GET /v1/reports/orders.sum" in {

    "with no interval" should {
      "with no group by" should {
        val emptyAggregate = OrderAggregate(count = 3)

        val fullAggregate =
          OrderAggregate(
            count = 3,
            profit = Some(-11.20.$$$),
            revenue = Some(16.$$$),
            waitingTimeInSeconds = Some(5700),
          )

        assertNoField()

        assertFieldResultWhenNoItems(
          "profit",
          ReportFields(key = None, OrderAggregate(count = 0, profit = Some(0.$$$))),
        )

        assertFieldResult("profit", ReportFields(key = None, emptyAggregate.copy(profit = fullAggregate.profit)))

        assertFieldResult("revenue", ReportFields(key = None, emptyAggregate.copy(revenue = fullAggregate.revenue)))

        assertFieldResult(
          "waiting_time",
          ReportFields(key = None, emptyAggregate.copy(waitingTimeInSeconds = fullAggregate.waitingTimeInSeconds)),
        )

        assertAllFieldsResult(ReportFields(key = None, fullAggregate))
      }

      "with group by" should {
        assertGroupByResult(
          "payment_type",
          ReportFields(
            key = OrderPaymentType.Cash,
            OrderAggregate(
              count = 1,
              profit = Some(-0.7.$$$),
              revenue = Some(1.$$$),
              waitingTimeInSeconds = Some(1800),
            ),
          ),
          ReportFields(
            key = OrderPaymentType.CreditCard,
            OrderAggregate(
              count = 1,
              profit = Some(-2.3.$$$),
              revenue = Some(10.$$$),
              waitingTimeInSeconds = Some(3600),
            ),
          ),
          ReportFields(
            key = OrderPaymentType.DebitCard,
            OrderAggregate(count = 1, profit = Some(-8.2.$$$), revenue = Some(5.$$$), waitingTimeInSeconds = Some(300)),
          ),
        )

        assertGroupByResult(
          "order_type",
          ReportFields(
            key = OrderType.DineIn,
            OrderAggregate(
              count = 2,
              profit = Some(-10.5.$$$),
              revenue = Some(15.$$$),
              waitingTimeInSeconds = Some(3900),
            ),
          ),
          ReportFields(
            key = OrderType.InStore,
            OrderAggregate(
              count = 1,
              profit = Some(-0.7.$$$),
              revenue = Some(1.$$$),
              waitingTimeInSeconds = Some(1800),
            ),
          ),
        )

        assertGroupByResult(
          "source_type",
          ReportFields(
            key = Source.Register,
            OrderAggregate(
              count = 1,
              profit = Some(-2.3.$$$),
              revenue = Some(10.$$$),
              waitingTimeInSeconds = Some(3600),
            ),
          ),
          ReportFields(
            key = Source.Storefront,
            OrderAggregate(
              count = 2,
              profit = Some(-8.9.$$$),
              revenue = Some(6.$$$),
              waitingTimeInSeconds = Some(2100),
            ),
          ),
        )

        assertGroupByResult(
          "feedback",
          ReportFields(
            key = Some("3"),
            OrderAggregate(count = 1, profit = Some(-8.2.$$$), revenue = Some(5.$$$), waitingTimeInSeconds = Some(300)),
          ),
          ReportFields(
            key = Some("5"),
            OrderAggregate(
              count = 1,
              profit = Some(-2.3.$$$),
              revenue = Some(10.$$$),
              waitingTimeInSeconds = Some(3600),
            ),
          ),
          ReportFields(
            key = None,
            OrderAggregate(
              count = 1,
              profit = Some(-0.7.$$$),
              revenue = Some(1.$$$),
              waitingTimeInSeconds = Some(1800),
            ),
          ),
        )
      }
    }

    "with interval" should {
      "with no group by" should {

        "with no fields it should reject the request" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval").addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field profit" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=profit")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      profit = Some(-11.20.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with field revenue" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=revenue")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      revenue = Some(16.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with field waiting time" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=waiting_time")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      waitingTimeInSeconds = Some(5700),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with all the fields" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      profit = Some(-11.20.$$$),
                      Some(16.$$$),
                      Some(5700),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by payment type" should {
        "with all the fields" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=payment_type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    OrderPaymentType.Cash,
                    OrderAggregate(count = 1, profit = Some(-0.7.$$$), Some(1.$$$), Some(1800)),
                  ),
                  ReportFields(
                    OrderPaymentType.CreditCard,
                    OrderAggregate(count = 1, profit = Some(-2.3.$$$), Some(10.$$$), Some(3600)),
                  ),
                  ReportFields(
                    OrderPaymentType.DebitCard,
                    OrderAggregate(count = 1, profit = Some(-8.2.$$$), Some(5.$$$), Some(300)),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by order type" should {
        "with all the fields" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=order_type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    OrderType.DineIn,
                    OrderAggregate(count = 2, profit = Some(-10.50.$$$), Some(15.$$$), Some(3900)),
                  ),
                  ReportFields(
                    OrderType.InStore,
                    OrderAggregate(count = 1, profit = Some(-0.7.$$$), Some(1.$$$), Some(1800)),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by source type" should {
        "with all the fields" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&&group_by=source_type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    Source.Register,
                    OrderAggregate(count = 1, profit = Some(-2.3.$$$), Some(10.$$$), Some(3600)),
                  ),
                  ReportFields(
                    Source.Storefront,
                    OrderAggregate(count = 2, profit = Some(-8.9.$$$), Some(6.$$$), Some(2100)),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by feedback" should {
        "with all the fields" in new OrdersSumFSpecContext {
          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&&group_by=feedback")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    Some("3"),
                    OrderAggregate(count = 1, profit = Some(-8.2.$$$), Some(5.00.$$$), Some(300)),
                  ),
                  ReportFields(Some("5"), OrderAggregate(count = 1, profit = Some(-2.3.$$$), Some(10.$$$), Some(3600))),
                  ReportFields(None, OrderAggregate(count = 1, profit = Some(-0.7.$$$), Some(1.$$$), Some(1800))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                      Some(0.$$$),
                      Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location_id" should {
      "return the correct data" in new OrdersSumFSpecContext {
        Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 1,
                    profit = Some(-8.2.$$$),
                    Some(5.$$$),
                    Some(300),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                    Some(0.$$$),
                    Some(0),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(14), end.plusDays(14)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                    Some(0.$$$),
                    Some(0),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(21), end.plusDays(21)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                    Some(0.$$$),
                    Some(0),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(28), end.plusDays(28)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                    Some(0.$$$),
                    Some(0),
                  ),
                ),
              ),
            ),
          )
          result ==== expectedResult
        }
      }

      "with a non-accessible location" should {
        "reject the request" in new OrdersSumFSpecContext {
          val newYork = Factory.location(merchant).create

          Get(s"/v1/reports/orders.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${newYork.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
