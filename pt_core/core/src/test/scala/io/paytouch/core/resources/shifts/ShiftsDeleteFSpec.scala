package io.paytouch.core.resources.shifts

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ShiftsDeleteFSpec extends ShiftsFSpec {

  "POST /v1/shifts.delete" in {
    "if request has valid token" in {
      "if shift belongs to a location accessible to the current user" should {
        "delete a shift" in new ShiftResourceFSpecContext {
          val shift = Factory.shift(user, rome).create

          Post(s"/v1/shifts.delete", Ids(ids = Seq(shift.id))).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            shiftDao.findById(shift.id).await should beEmpty
          }
        }
      }

      "if shift belongs to a location NOT accessible to the current user" should {
        "NOT delete a shift" in new ShiftResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val otherUser = Factory.user(merchant, locations = Seq(newYork)).create
          val shift = Factory.shift(otherUser, newYork).create

          Post(s"/v1/shifts.delete", Ids(ids = Seq(shift.id))).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            shiftDao.findById(shift.id).await should beSome
          }
        }
      }

    }
  }
}
