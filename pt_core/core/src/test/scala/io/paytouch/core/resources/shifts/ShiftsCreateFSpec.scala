package io.paytouch.core.resources.shifts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ShiftsCreateFSpec extends ShiftsFSpec {

  abstract class ShiftsCreateFSpecContext extends ShiftResourceFSpecContext

  "POST /v1/shifts.create" in {
    "if request has valid token" in {

      "create shift and return 201" in new ShiftsCreateFSpecContext {
        val newShiftId = UUID.randomUUID
        val creation = random[ShiftCreation].copy(userId = user.id, locationId = rome.id)

        Post(s"/v1/shifts.create?shift_id=$newShiftId", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val shift = responseAs[ApiResponse[Shift]].data
          assertCreation(newShiftId, creation)
          assertResponseById(newShiftId, shift)
        }
      }
    }

    "if request has invalid user id" should {
      "return 404" in new ShiftsCreateFSpecContext {
        val newShiftId = UUID.randomUUID
        val creation = random[ShiftCreation].copy(userId = UUID.randomUUID, locationId = rome.id)
        Post(s"/v1/shifts.create?shift_id=$newShiftId", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid location id" should {
      "return 404" in new ShiftsCreateFSpecContext {
        val newShiftId = UUID.randomUUID
        val creation = random[ShiftCreation].copy(userId = user.id, locationId = UUID.randomUUID)
        Post(s"/v1/shifts.create?shift_id=$newShiftId", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if user id is not associated to location id" should {
      "return 404" in new ShiftsCreateFSpecContext {
        val newYork = Factory.location(merchant).create
        val newShiftId = UUID.randomUUID
        val creation = random[ShiftCreation].copy(userId = user.id, locationId = newYork.id)
        Post(s"/v1/shifts.create?shift_id=$newShiftId", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ShiftsCreateFSpecContext {
        val newShiftId = UUID.randomUUID
        val creation = random[ShiftCreation]
        Post(s"/v1/shifts.create?shift_id=$newShiftId", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
