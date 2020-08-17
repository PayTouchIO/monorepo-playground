package io.paytouch.core.resources.timecards

import java.time.DayOfWeek._
import java.time._
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TimeCardsCreateFSpec extends TimeCardsFSpec {

  abstract class TimeCardsCreateFSpecContext extends TimeCardResourceFSpecContext {
    val totalMinInTimeCard = Duration.ofHours(4).toMinutes.toInt // 240
  }

  abstract class TimeCardsCreateWithOverlappingShift extends TimeCardsCreateFSpecContext with OverlappingShiftFixtures
  abstract class TimeCardsCreateWithRegularShift extends TimeCardsCreateFSpecContext with RegularShiftFixtures

  "POST /v1/time_cards.create" in {
    "if request has valid token" in {

      "create time card not associated to any shift" in {

        "if shift is overlapping between days" in {

          /*
           * SHIFT |----|
           * TC            |----|
           */
          "with no shift associated if shift is before" in new TimeCardsCreateWithOverlappingShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.plusHours(12), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = 0,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== None
            }
          }

          /*
           * SHIFT         |----|
           * TC    |----|
           */
          "with no shift associated if shift is after" in new TimeCardsCreateWithOverlappingShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.minusHours(12), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = 0,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== None
            }
          }

          /*
           * SHIFT |--------|
           * TC      |----|
           */
          "with shift associated if shift covers the time card" in new TimeCardsCreateWithOverlappingShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.plusHours(3), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = totalMinInTimeCard - shiftDurationMinutes,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          /*
           * SHIFT   |----|
           * TC    |--------|
           */
          "with shift associated if shift is covered by the time card" in new TimeCardsCreateWithOverlappingShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.minusHours(3), rome.timezone)
            val endAt = ZonedDateTime.of(tuesdayWithinRange.plusDays(1), shiftEndTime.plusHours(3), rome.timezone)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              val `6hoursInMin` = Duration.ofHours(6).toMinutes.toInt
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = `6hoursInMin`,
                totalMins = Some(shiftDurationMinutes + `6hoursInMin`),
                regularMins = Some(shiftDurationMinutes),
                overtimeMins = Some(`6hoursInMin`),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          /*
           * SHIFT   |----|
           * TC    |----|
           */
          "with shift associated if shift overlaps the time card beginning" in new TimeCardsCreateWithOverlappingShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.minusHours(3), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = totalMinInTimeCard - shiftDurationMinutes,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          /*
           * SHIFT |----|
           * TC      |----|
           */
          "with shift associated if shift overlaps the time card ending" in new TimeCardsCreateWithOverlappingShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftEndTime.minusHours(3), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = totalMinInTimeCard - shiftDurationMinutes,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }
        }

        "if shift is NOT overlapping between days" in {

          /*
           * SHIFT |----|
           * TC            |----|
           */
          "with no shift associated if shift is before" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.plusHours(12), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = 0,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== None
            }
          }

          /*
           * SHIFT         |----|
           * TC    |----|
           */
          "with no shift associated if shift is after" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.minusHours(12), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = 0,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== None
            }
          }

          /*
           * SHIFT |--------|
           * TC      |----|
           */
          "with shift associated if shift covers the time card" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.plusHours(3), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = totalMinInTimeCard - shiftDurationMinutes,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          /*
           * SHIFT   |----|
           * TC    |--------|
           */
          "with shift associated if shift is covered by the time card" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.minusHours(3), rome.timezone)
            val endAt = ZonedDateTime.of(tuesdayWithinRange, shiftEndTime.plusHours(3), rome.timezone)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              val `6hoursInMin` = Duration.ofHours(6).toMinutes.toInt
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = `6hoursInMin`,
                totalMins = Some(shiftDurationMinutes + `6hoursInMin`),
                regularMins = Some(shiftDurationMinutes),
                overtimeMins = Some(`6hoursInMin`),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          /*
           * SHIFT   |----|
           * TC    |----|
           */
          "with shift associated if shift overlaps the time card beginning" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.minusHours(3), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = totalMinInTimeCard - shiftDurationMinutes,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          /*
           * SHIFT |----|
           * TC      |----|
           */
          "with shift associated if shift overlaps the time card ending" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID
            val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftEndTime.minusHours(3), rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = totalMinInTimeCard - shiftDurationMinutes,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
            }
          }

          "with no shift covering the date" in new TimeCardsCreateWithRegularShift {
            val newTimeCardId = UUID.randomUUID

            val startAt = ZonedDateTime.of(tuesdayOutsideRange, shiftStartTime, rome.timezone)
            val endAt = startAt.plusMinutes(totalMinInTimeCard)
            val creation = random[TimeCardCreation]
              .copy(
                userId = user.id,
                locationId = rome.id,
                shiftId = None,
                startAt = Some(startAt),
                endAt = Some(endAt),
              )

            Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusCreated()

              val timeCard = responseAs[ApiResponse[TimeCard]].data
              assertCreation(
                newTimeCardId,
                creation,
                deltaMins = 0,
                totalMins = Some(totalMinInTimeCard),
                regularMins = Some(totalMinInTimeCard),
                overtimeMins = Some(0),
              )
              assertResponseById(newTimeCardId, timeCard)
              timeCardDao.findById(newTimeCardId).await.get.shiftId ==== None
            }
          }
        }

        "test with data from live environments" in new TimeCardsCreateFSpecContext {

          /**
            * Timecard start/end datetimes are sent to core in UTC and should be converted to location timezone
            * when matching against a shift.
            * When the timezone hour difference is larger than a shift length, the matching would fail if conversion
            * doesn't take place.
            * Specs above do not hit this edgecase (their purpose is to validate the various cases of time overlaps,
            * hence the following manually crafted spec.
            */
          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          Factory.userLocation(user, newYork).create

          val shiftStartTime = LocalTime.of(9, 0)
          val shiftEndTime = LocalTime.of(10, 0)
          val shiftDurationMinutes = Duration.ofHours(1).toMinutes.toInt
          val shiftStartDate = LocalDate.of(2017, 1, 1)
          val shiftEndDate = LocalDate.of(2018, 1, 1)
          val tuesdayWithinRange = LocalDate.of(2017, 5, 2)
          val shiftDays = Seq(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
          val shift = Factory
            .shift(
              user,
              newYork,
              startDate = Some(shiftStartDate),
              endDate = Some(shiftEndDate),
              startTime = Some(shiftStartTime),
              endTime = Some(shiftEndTime),
              days = shiftDays,
              repeat = Some(true),
            )
            .create

          val newTimeCardId = UUID.randomUUID
          val startAt = ZonedDateTime.of(tuesdayWithinRange, shiftStartTime.plusHours(5), ZoneId.of("UTC"))
          val endAt = startAt.plusMinutes(totalMinInTimeCard)
          val creation = random[TimeCardCreation]
            .copy(
              userId = user.id,
              locationId = newYork.id,
              shiftId = None,
              startAt = Some(startAt),
              endAt = Some(endAt),
            )

          Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val timeCard = responseAs[ApiResponse[TimeCard]].data
            assertCreation(
              newTimeCardId,
              creation,
              deltaMins = 180,
              totalMins = Some(totalMinInTimeCard),
              regularMins = Some(shiftDurationMinutes),
              overtimeMins = Some(180),
            )
            assertResponseById(newTimeCardId, timeCard)
            timeCardDao.findById(newTimeCardId).await.get.shiftId ==== Some(shift.id)
          }
        }
      }

      "create time card associated to a shift and return 201" in new TimeCardsCreateWithOverlappingShift {
        val newTimeCardId = UUID.randomUUID
        val endAt = UtcTime.now
        val startAt = endAt.minusMinutes(totalMinInTimeCard)
        val creation = random[TimeCardCreation].copy(
          userId = user.id,
          locationId = rome.id,
          shiftId = Some(shift.id),
          startAt = Some(startAt),
          endAt = Some(endAt),
        )

        Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val timeCard = responseAs[ApiResponse[TimeCard]].data
          assertCreation(
            newTimeCardId,
            creation,
            deltaMins = totalMinInTimeCard - shift.totalMins,
            totalMins = Some(totalMinInTimeCard),
            regularMins = Some(totalMinInTimeCard),
            overtimeMins = Some(0),
          )
          assertResponseById(newTimeCardId, timeCard)
        }
      }

      "if time card starts in the future" should {
        "return 400" in new TimeCardsCreateWithOverlappingShift {
          val newTimeCardId = UUID.randomUUID

          val startAt = UtcTime.now.plusHours(3)
          val endAt = startAt.plusHours(3)
          val creation = random[TimeCardCreation].copy(
            userId = user.id,
            locationId = rome.id,
            shiftId = Some(shift.id),
            startAt = Some(startAt),
            endAt = Some(endAt),
          )

          Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidFutureTime")
          }
        }
      }

      "if time card ends in the future" should {
        "return 400" in new TimeCardsCreateWithOverlappingShift {
          val newTimeCardId = UUID.randomUUID

          val startAt = UtcTime.now.minusHours(3)
          val endAt = startAt.plusHours(5)
          val creation = random[TimeCardCreation].copy(
            userId = user.id,
            locationId = rome.id,
            shiftId = Some(shift.id),
            startAt = Some(startAt),
            endAt = Some(endAt),
          )

          Post(s"/v1/time_cards.create?time_card_id=$newTimeCardId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidFutureTime")
          }
        }
      }

      "if request has invalid user id" should {
        "return 404" in new TimeCardsCreateWithOverlappingShift {
          val randomUuid = UUID.randomUUID
          val creation =
            random[TimeCardCreation].copy(
              userId = UUID.randomUUID,
              locationId = rome.id,
              shiftId = Some(shift.id),
              startAt = None,
              endAt = None,
            )
          Post(s"/v1/time_cards.create?time_card_id=$randomUuid", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if request has invalid location id" should {
        "return 404" in new TimeCardsCreateWithOverlappingShift {
          val randomUuid = UUID.randomUUID
          val creation =
            random[TimeCardCreation].copy(
              userId = user.id,
              locationId = UUID.randomUUID,
              shiftId = Some(shift.id),
              startAt = None,
              endAt = None,
            )
          Post(s"/v1/time_cards.create?time_card_id=$randomUuid", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if request has invalid shift id" should {
        "return 404" in new TimeCardsCreateFSpecContext {
          val randomUuid = UUID.randomUUID
          val creation =
            random[TimeCardCreation].copy(
              userId = user.id,
              locationId = rome.id,
              shiftId = Some(UUID.randomUUID),
              startAt = None,
              endAt = None,
            )
          Post(s"/v1/time_cards.create?time_card_id=$randomUuid", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TimeCardsCreateFSpecContext {
        val randomUuid = UUID.randomUUID
        val creation = random[TimeCardCreation]
        Post(s"/v1/time_cards.create?time_card_id=$randomUuid", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
