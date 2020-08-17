package io.paytouch.core.resources.payroll

import java.time.{ LocalDateTime, LocalTime, ZonedDateTime }

import io.paytouch.core.data.model.enums.OrderStatus
import io.paytouch.core.data.model.{ LocationRecord, ShiftRecord, UserRecord }
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Payroll => PayrollEntity, _ }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class PayrollListFSpec extends PayrollFSpec {

  abstract class PayrollListFSpecContext extends PayrollResourceFSpecContext {
    val defaultStartTime = LocalTime.parse("16:32:31")
    val now = UtcTime.ofLocalDateTime(LocalDateTime.parse("2017-06-19T16:32:31"))
    val yesterday = now.minusDays(1)

    def createTimeCardWithShift(
        user: UserRecord,
        location: LocationRecord,
        timeCardTotalMins: Int,
        shiftTotalMins: Int,
        timeCardStartAt: Option[ZonedDateTime] = None,
      ) = {
      val shift = Factory
        .shift(
          user,
          location,
          startTime = Some(defaultStartTime),
          endTime = Some(defaultStartTime.plusMinutes(shiftTotalMins)),
        )
        .create

      Factory
        .timeCard(
          user,
          location,
          totalMins = timeCardTotalMins,
          startAt = timeCardStartAt.orElse(Some(now)).map(_.minusMinutes(timeCardTotalMins)),
          endAt = Some(now),
          shift = Some(shift),
        )
        .create
    }

    override lazy val user = Factory
      .user(
        merchant,
        firstName = Some("John"),
        lastName = Some("Doe"),
        password = Some(password),
        locations = locations,
        userRole = Some(userRole),
        hourlyRateAmount = Some(25),
        overtimeRateAmount = Some(30),
        overrideNow = Some(now),
      )
      .create
  }

  "GET /v1/payroll.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all payrolls of users in locations accessible to the current user" in new PayrollListFSpecContext {
          val newYork = Factory.location(merchant).create
          val userInNewYork = Factory.user(merchant, locations = Seq(newYork)).create

          val timeCard1ForUser =
            createTimeCardWithShift(user, rome, shiftTotalMins = 160, timeCardTotalMins = 180)
          val timeCard2ForUser =
            createTimeCardWithShift(user, rome, shiftTotalMins = 110, timeCardTotalMins = 120)

          val userInLondon = Factory
            .user(
              merchant,
              firstName = Some("Harry"),
              lastName = Some("Potter"),
              locations = Seq(london),
              hourlyRateAmount = Some(40),
              overtimeRateAmount = Some(50),
            )
            .create

          val timeCard1ForUserInLondon =
            createTimeCardWithShift(userInLondon, london, shiftTotalMins = 80, timeCardTotalMins = 60)
          val timeCard2ForUserInLondon = createTimeCardWithShift(
            userInLondon,
            london,
            shiftTotalMins = 288,
            timeCardTotalMins = 300,
          )

          val order = Factory
            .order(
              merchant,
              tipAmount = Some(10),
              user = Some(userInLondon),
              status = Some(OrderStatus.Completed),
              location = Some(london),
            )
            .create

          Get("/v1/payroll.list").addHeader(authorizationHeader) ~> routes ~> check {
            val payrolls = responseAs[PaginatedApiResponse[Seq[PayrollEntity]]]
            payrolls.data.map(_.user.id) ==== Seq(userInLondon.id, user.id)
            assertResponse(
              userInLondon,
              payrolls.data.find(_.user.id == userInLondon.id).get,
              totalMins = Some(360),
              totalDeltaMins = Some(-8),
              totalWage = Some(242.$$$),
              totalTips = Some(Seq(10.$$$)),
              totalRegularMins = Some(60 + 288),
              totalOtMins = Some(0 + 12),
            )
            assertResponse(
              user,
              payrolls.data.find(_.user.id == user.id).get,
              totalMins = Some(300),
              totalDeltaMins = Some(30),
              totalWage = Some(127.5.$$$),
              totalTips = Some(Seq.empty),
              totalRegularMins = Some(160 + 110),
              totalOtMins = Some(20 + 10),
            )
          }
        }
      }

      "with q parameter" should {
        "return a paginated list of all payroll users filtered by query matching first/last name" in new PayrollListFSpecContext {
          val matchingUser = Factory
            .user(merchant, firstName = Some("Barack"), lastName = Some("Obama"), locations = Seq(london))
            .create

          Get("/v1/payroll.list?q=obama").addHeader(authorizationHeader) ~> routes ~> check {
            val payrolls = responseAs[PaginatedApiResponse[Seq[PayrollEntity]]]
            payrolls.data.map(_.user.id) ==== Seq(matchingUser.id)
          }
        }
      }

      "with location_id parameter" should {
        "return a paginated list of all payroll users filtered by users belonging to the given location, with aggregated data from time cards and orders for the given location" in new PayrollListFSpecContext {
          val userInLondon = Factory.user(merchant, locations = Seq(london)).create

          val timeCardRomeForUser =
            createTimeCardWithShift(user, rome, shiftTotalMins = 150, timeCardTotalMins = 180)
          val timeCardLondonForUser =
            createTimeCardWithShift(user, london, shiftTotalMins = 110, timeCardTotalMins = 120)

          val orderInRome = Factory
            .order(
              merchant,
              tipAmount = Some(10),
              user = Some(user),
              status = Some(OrderStatus.Completed),
              location = Some(rome),
            )
            .create

          val orderInLondon = Factory
            .order(
              merchant,
              tipAmount = Some(2),
              user = Some(user),
              status = Some(OrderStatus.Completed),
              location = Some(london),
            )
            .create

          Get(s"/v1/payroll.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val payrolls = responseAs[PaginatedApiResponse[Seq[PayrollEntity]]]
            payrolls.data.map(_.user.id) ==== Seq(user.id)
            assertResponse(
              user,
              payrolls.data.find(_.user.id == user.id).get,
              totalMins = Some(180),
              totalDeltaMins = Some(30),
              totalWage = Some(77.5.$$$),
              totalTips = Some(Seq(10.$$$)),
            )
          }
        }
      }

      "with from/to" should {
        "return a paginated list of all payroll users with aggregated data from time cards and orders in the given time range" in new PayrollListFSpecContext {
          val timeCardTodayForUser =
            createTimeCardWithShift(user, rome, shiftTotalMins = 150, timeCardTotalMins = 180)
          val timeCardYesterdayForUser =
            createTimeCardWithShift(
              user,
              london,
              shiftTotalMins = 110,
              timeCardTotalMins = 120,
              timeCardStartAt = Some(yesterday),
            )

          val orderInRome = Factory
            .order(
              merchant,
              tipAmount = Some(10),
              user = Some(user),
              status = Some(OrderStatus.Completed),
              location = Some(rome),
              completedAt = Some(now),
            )
            .create

          val orderInLondon = Factory
            .order(
              merchant,
              tipAmount = Some(2),
              user = Some(user),
              status = Some(OrderStatus.Completed),
              location = Some(london),
              completedAt = Some(yesterday),
            )
            .create

          Get(s"/v1/payroll.list?from=${now.toLocalDate}&to=${now.plusDays(1).toLocalDate}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val payrolls = responseAs[PaginatedApiResponse[Seq[PayrollEntity]]]
            payrolls.data.map(_.user.id) ==== Seq(user.id)
            assertResponse(
              user,
              payrolls.data.find(_.user.id == user.id).get,
              totalMins = Some(180),
              totalDeltaMins = Some(30),
              totalWage = Some(77.5.$$$),
              totalTips = Some(Seq(10.$$$)),
            )
          }
        }
      }
    }
  }

}
