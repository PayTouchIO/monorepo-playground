package io.paytouch.core.resources.shifts

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ Shift => ShiftEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ShiftsGetFSpec extends ShiftsFSpec {

  "GET /v1/shifts.get?shift_id=<shift-id>" in {
    "if request has valid token" in {

      "if the shift belongs to the merchant" in {
        "if the shift belongs to a location accessible to the current user" should {
          "return a shift" in new ShiftResourceFSpecContext {
            val shift = Factory.shift(user, rome).create

            Get(s"/v1/shifts.get?shift_id=${shift.id}").addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ShiftEntity]].data
              assertResponse(shift, entity)
            }
          }
        }

        "with expand[]=location" should {
          "return the shift with expanded location" in new ShiftResourceFSpecContext {
            val shift = Factory.shift(user, rome).create

            Get(s"/v1/shifts.get?shift_id=${shift.id}&expand[]=locations")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[ShiftEntity]].data
              assertResponse(shift, entity, location = Some(rome))
            }
          }
        }

        "if the shift doesn't belong to a location accessible to the current user" should {
          "return 404" in new ShiftResourceFSpecContext {
            val newYork = Factory.location(merchant).create
            val newYorkUser = Factory.user(merchant, locations = Seq(newYork)).create
            val unaccessibleShift = Factory.shift(newYorkUser, newYork).create

            Get(s"/v1/shifts.get?shift_id=${unaccessibleShift.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }

      "if the shift does not belong to the merchant" should {
        "return 404" in new ShiftResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorShift = Factory.shift(competitorUser, competitorLocation).create

          Get(s"/v1/shifts.get?shift_id=${competitorShift.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
