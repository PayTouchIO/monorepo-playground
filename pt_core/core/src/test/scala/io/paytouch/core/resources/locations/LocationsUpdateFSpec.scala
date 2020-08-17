package io.paytouch.core.resources.locations

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.entities.{ Location => LocationEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LocationsUpdateFSpec extends LocationsFSpec {

  abstract class LocationsUpdateFSpecContext extends LocationsResourceFSpecContext

  "POST /v1/locations.update?location_id=<location-id>" in {
    "if request has valid token" in {
      "if location doesn't exist" should {
        "return 404" in new LocationsUpdateFSpecContext {
          val locationId = UUID.randomUUID
          val locationUpdate = random[LocationUpdate]

          Post(s"/v1/locations.update?location_id=$locationId", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if location exists and is  deleted" should {
        "return 404" in new LocationsUpdateFSpecContext {
          val locationUpdate = random[LocationUpdate]
          locationDao.deleteByIdsAndMerchantId(Seq(rome.id), merchant.id).await

          Post(s"/v1/locations.update?location_id=${rome.id}", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if location exists and is not deleted" should {
        "update location and return 200" in new LocationsUpdateFSpecContext {
          val availabilities = Seq(Availability(LocalTime.of(12, 34, 0), LocalTime.of(21, 43, 35)))
          val openingHours = Map(Weekdays.Monday -> availabilities, Weekdays.Tuesday -> availabilities)

          val locationUpdate =
            random[LocationUpdate].copy(openingHours = Some(openingHours), email = Some(randomEmail))

          Post(s"/v1/locations.update?location_id=${rome.id}", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val locationResponse = responseAs[ApiResponse[LocationEntity]].data
            assertUpdate(locationUpdate, rome.id)
            assertResponseById(locationResponse, rome.id)

            assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupLocations)
          }
        }

        "keeps dummyData = true on the location when it is updated if not sent" in new LocationsUpdateFSpecContext {
          val dummyDataLocation = Factory.location(merchant, dummyData = Some(true)).create
          override lazy val locations = Seq(dummyDataLocation)
          dummyDataLocation.dummyData ==== true

          val locationUpdate = random[LocationUpdate].copy(email = Some(randomEmail), dummyData = None)

          Post(s"/v1/locations.update?location_id=${dummyDataLocation.id}", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val locationResponse = responseAs[ApiResponse[LocationEntity]].data
            locationResponse.dummyData ==== true
          }
        }

        "sets dummyData = false on the location when it is updated if sent" in new LocationsUpdateFSpecContext {
          val dummyDataLocation = Factory.location(merchant, dummyData = Some(true)).create
          override lazy val locations = Seq(dummyDataLocation)
          dummyDataLocation.dummyData ==== true

          val locationUpdate = random[LocationUpdate].copy(email = Some(randomEmail), dummyData = Some(false))

          Post(s"/v1/locations.update?location_id=${dummyDataLocation.id}", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val locationResponse = responseAs[ApiResponse[LocationEntity]].data
            locationResponse.dummyData ==== false
          }
        }
      }

      "if location email is invalid" should {
        "return 400" in new LocationsUpdateFSpecContext {
          val locationUpdate = random[LocationUpdate].copy(openingHours = None, email = Some("yadda"))

          Post(s"/v1/locations.update?location_id=${rome.id}", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }

      "if location is updated settings should be retained" should {
        "update existing location and retain settings 200" in new LocationsUpdateFSpecContext {

          @scala.annotation.nowarn("msg=Auto-application")
          val settingsUpdate = random[LocationSettingsUpdate].copy(
            orderRoutingAuto = Some(true),
          )

          Post(s"/v1/locations.update_settings?location_id=${london.id}", settingsUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }

          @scala.annotation.nowarn("msg=Auto-application")
          val settingsUpdate2 = random[LocationSettingsUpdate].copy(
            orderRoutingAuto = Some(false),
          )

          Post(s"/v1/locations.update_settings?location_id=${rome.id}", settingsUpdate2)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }

          val locationUpdate = random[LocationUpdate].copy(email = Some(randomEmail))

          Post(s"/v1/locations.update?location_id=${london.id}", locationUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val settings = locationSettingsDao.findByLocationId(london.id, london.merchantId).await.head
            settings.orderRoutingAuto === true
          }
        }
      }
    }
  }
}
