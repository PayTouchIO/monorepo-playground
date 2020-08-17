package io.paytouch.core.resources.shifts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.ShiftRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ShiftsUpdateFSpec extends ShiftsFSpec {

  abstract class ShiftsUpdateFSpecContext extends ShiftResourceFSpecContext {
    def assertShiftWasntChanged(originalShift: ShiftRecord) = {
      val dbShift = shiftDao.findById(originalShift.id).await.get
      dbShift ==== originalShift
    }
  }

  "POST /v1/shifts.update?shift_id=$" in {
    "if request has valid token" in {
      "if shift belong to same merchant" should {
        "update shift and return 200" in new ShiftsUpdateFSpecContext {
          val userInSameLocation = Factory.user(merchant, locations = Seq(rome)).create
          val shift = Factory.shift(user, rome).create
          val update = random[ShiftUpdate].copy(userId = Some(userInSameLocation.id), locationId = Some(rome.id))

          Post(s"/v1/shifts.update?shift_id=${shift.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(shift.id, update)
          }
        }
      }
      "if shift doesn't belong to a location accessible by the current user" should {
        "not update shift and return 404" in new ShiftsUpdateFSpecContext {
          val newYork = Factory.location(merchant).create
          val userInNewYork = Factory.user(merchant, locations = Seq(newYork)).create
          val shift = Factory.shift(userInNewYork, newYork).create
          val update = random[ShiftUpdate].copy(userId = Some(userInNewYork.id), locationId = Some(rome.id))
          Post(s"/v1/shifts.update?shift_id=${shift.id}", update).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertShiftWasntChanged(shift)
          }
        }
      }
      "if shift doesn't belong to current user's merchant" in {
        "not update shift and return 404" in new ShiftsUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorShift = Factory.shift(competitorUser, competitorLocation).create

          val update =
            random[ShiftUpdate].copy(userId = Some(competitorUser.id), locationId = Some(competitorLocation.id))

          Post(s"/v1/shifts.update?shift_id=${competitorShift.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertShiftWasntChanged(competitorShift)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ShiftsUpdateFSpecContext {
        val shiftId = UUID.randomUUID
        val update = random[ShiftUpdate]
        Post(s"/v1/shifts.update?shift_id=$shiftId", update).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
