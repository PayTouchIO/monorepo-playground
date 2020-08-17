package io.paytouch.core.resources.shifts

import java.time.LocalDate

import io.paytouch.core.data.model.enums.ShiftStatus
import io.paytouch.core.entities.{ Shift => ShiftEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ShiftsListFSpec extends ShiftsFSpec {

  "GET /v1/shifts.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all shifts of users in locations accessible to the current user" in new ShiftResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val userInNewYork = Factory.user(merchant, locations = Seq(newYork)).create

          val shift1 = Factory.shift(user, rome).create
          val shift2 = Factory.shift(user, rome).create
          val shift3 = Factory.shift(user, rome).create
          val shift4 = Factory.shift(userInNewYork, newYork).create

          Get("/v1/shifts.list").addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(shift1.id, shift2.id, shift3.id)
            assertResponse(shift1, shifts.data.find(_.id == shift1.id).get)
            assertResponse(shift2, shifts.data.find(_.id == shift2.id).get)
            assertResponse(shift3, shifts.data.find(_.id == shift3.id).get)
          }
        }
      }

      "with location_id parameter" should {
        "return a paginated list of all shifts filtered by shifts belonging to the given location" in new ShiftResourceFSpecContext {
          val userInRome = Factory.user(merchant, locations = Seq(rome)).create
          val userInLondon = Factory.user(merchant, locations = Seq(london)).create
          val shift1 = Factory.shift(user, rome).create
          val shift2 = Factory.shift(userInRome, rome).create
          val shift3 = Factory.shift(userInLondon, london).create

          Get(s"/v1/shifts.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(shift1.id, shift2.id)
            assertResponse(shift1, shifts.data.find(_.id == shift1.id).get)
            assertResponse(shift2, shifts.data.find(_.id == shift2.id).get)
          }
        }
      }

      "with user_role_id parameter" should {
        "return a paginated list of all shifts filtered by user with the given role" in new ShiftResourceFSpecContext {
          val userInRome = Factory.user(merchant, locations = Seq(rome)).create
          val userInLondon = Factory.user(merchant, locations = Seq(london), userRole = Some(userRole)).create
          val shift1 = Factory.shift(user, rome).create
          val shift2 = Factory.shift(userInRome, rome).create
          val shift3 = Factory.shift(userInLondon, london).create

          Get(s"/v1/shifts.list?user_role_id=${userRole.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(shift1.id, shift3.id)
            assertResponse(shift1, shifts.data.find(_.id == shift1.id).get)
            assertResponse(shift3, shifts.data.find(_.id == shift3.id).get)
          }
        }
      }

      "with status parameter" should {
        "return a paginated list of all shifts filtered by shift status" in new ShiftResourceFSpecContext {
          val shift1 = Factory.shift(user, rome, status = Some(ShiftStatus.Draft)).create
          val shift2 = Factory.shift(user, rome, status = Some(ShiftStatus.Published)).create
          val shift3 = Factory.shift(user, rome, status = Some(ShiftStatus.Published)).create

          Get(s"/v1/shifts.list?status=published").addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(shift2.id, shift3.id)
            assertResponse(shift2, shifts.data.find(_.id == shift2.id).get)
            assertResponse(shift3, shifts.data.find(_.id == shift3.id).get)
          }
        }
      }

      "with from" should {
        "return a paginated list of all shifts that end after the given date" in new ShiftResourceFSpecContextForTimeRangeFilters {
          Get(s"/v1/shifts.list?from=$rangeStart")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(
              shiftCoveringRange,
              shiftStartingInRange,
              shiftEndingInRange,
              shiftFullyInRange,
              shiftAfterTimeRange,
            ).map(_.id)
          }
        }
      }

      "with to" should {
        "return a paginated list of all shifts that start before the given date" in new ShiftResourceFSpecContextForTimeRangeFilters {
          Get(s"/v1/shifts.list?to=$rangeEnd")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(
              shiftCoveringRange,
              shiftStartingInRange,
              shiftEndingInRange,
              shiftFullyInRange,
              shiftBeforeTimeRange,
            ).map(_.id)
          }
        }
      }

      "with from/to" should {
        "return a paginated list of all shifts active in the time range" in new ShiftResourceFSpecContextForTimeRangeFilters {

          Get(s"/v1/shifts.list?from=$rangeStart&to=$rangeEnd")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            shifts.data.map(_.id) ==== Seq(
              shiftCoveringRange,
              shiftStartingInRange,
              shiftEndingInRange,
              shiftFullyInRange,
            ).map(_.id)
          }
        }
      }

      "with expand[]=location" should {
        "return a list of shifts with expanded location" in new ShiftResourceFSpecContext {
          val shift = Factory.shift(user, rome).create

          Get(s"/v1/shifts.list?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            val shifts = responseAs[PaginatedApiResponse[Seq[ShiftEntity]]]
            assertResponse(shift, shifts.data.find(_.id == shift.id).get, location = Some(rome))
          }
        }
      }
    }
  }

  trait ShiftResourceFSpecContextForTimeRangeFilters extends ShiftResourceFSpecContext {
    val rangeStart = "2016-07-03"
    val rangeEnd = "2016-07-06"
    val shiftCoveringRange = Factory
      .shift(user, rome, startDate = Some(LocalDate.of(2016, 7, 1)), endDate = Some(LocalDate.of(2016, 7, 8)))
      .create
    val shiftStartingInRange = Factory
      .shift(user, rome, startDate = Some(LocalDate.of(2016, 7, 4)), endDate = Some(LocalDate.of(2016, 7, 8)))
      .create
    val shiftEndingInRange = Factory
      .shift(user, rome, startDate = Some(LocalDate.of(2016, 7, 1)), endDate = Some(LocalDate.of(2016, 7, 5)))
      .create
    val shiftFullyInRange = Factory
      .shift(user, rome, startDate = Some(LocalDate.of(2016, 7, 4)), endDate = Some(LocalDate.of(2016, 7, 5)))
      .create
    val shiftAfterTimeRange = Factory
      .shift(user, rome, startDate = Some(LocalDate.of(2016, 7, 8)), endDate = Some(LocalDate.of(2016, 7, 15)))
      .create
    val shiftBeforeTimeRange = Factory
      .shift(user, rome, startDate = Some(LocalDate.of(2016, 7, 1)), endDate = Some(LocalDate.of(2016, 7, 2)))
      .create

  }
}
