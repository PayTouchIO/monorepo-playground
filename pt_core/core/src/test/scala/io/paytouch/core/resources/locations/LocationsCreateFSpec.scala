package io.paytouch.core.resources.locations

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.enums.{ BusinessType, NextNumberType, ScopeType }
import io.paytouch.core.data.model.{ LocationSettingsRecord, NextNumberRecord }
import io.paytouch.core.entities.enums.{ MerchantSetupSteps, TipsHandlingMode }
import io.paytouch.core.entities.{ Location => LocationEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LocationsCreateFSpec extends LocationsFSpec {
  abstract class LocationsCreateFSpecContext extends LocationsResourceFSpecContext {
    val nextNumberDao = daos.nextNumberDao

    val newLocationId = UUID.randomUUID

    def assertNextOrderNumber(locationId: UUID, initialNumber: Int) = {
      val record =
        nextNumberDao.findByScopeAndType(Scope.fromLocationId(locationId), NextNumberType.Order).await
      record should beSome[NextNumberRecord]
      record.get.nextVal ==== initialNumber
    }

    def assertLocationSettingsCreated(locationId: UUID, businessType: BusinessType) = {
      val isRestaurant = businessType == BusinessType.Restaurant || businessType == BusinessType.QSR
      val isRetail = businessType == BusinessType.Retail

      val maybeSettings = locationSettingsDao.findByLocationId(locationId, merchant.id).await
      maybeSettings should beSome
      val settings = maybeSettings.get

      settings.locationId ==== locationId
      settings.orderRoutingAuto ==== isRestaurant
      settings.orderTypeDineIn ==== isRestaurant
      settings.orderTypeTakeOut ==== isRestaurant
      settings.orderTypeDeliveryRestaurant ==== isRestaurant
      settings.orderTypeInStore ==== true
      settings.orderTypeInStorePickUp ==== true
      settings.orderTypeDeliveryRetail ==== isRetail
      settings.webStorefrontActive ==== false
      settings.mobileStorefrontActive ==== false
      settings.facebookStorefrontActive ==== false
      settings.invoicesActive ==== false
      settings.discountBelowCostActive ==== false
      settings.cashDrawerManagementActive ==== true
      settings.giftCardsActive ==== true
      settings.paymentTypeCreditCard ==== true
      settings.paymentTypeCash ==== true
      settings.paymentTypeDebitCard ==== true
      settings.paymentTypeCheck ==== true
      settings.paymentTypeGiftCard ==== true
      settings.paymentTypeStoreCredit ==== false
      settings.paymentTypeEbt ==== false
      settings.paymentTypeApplePay ==== false
      settings.tipsHandling ==== TipsHandlingMode.TipJar
      settings.tipsOnDeviceEnabled ==== isRestaurant
      settings.bypassSignatureAmount ==== 0
      settings.onlineStorefrontEnabled ==== false
      settings.maxDrivingDistanceInMeters ==== None
    }

    def assertLocationSettingsCopiedFromPreviousLocation(locationId: UUID, latestSettings: LocationSettingsRecord) = {
      val maybeSettings = locationSettingsDao.findByLocationId(locationId, merchant.id).await
      maybeSettings should beSome

      val settings = maybeSettings.get
      settings.locationId ==== locationId
      settings.orderRoutingAuto ==== latestSettings.orderRoutingAuto
      settings.orderTypeDineIn ==== latestSettings.orderTypeDineIn
      settings.orderTypeTakeOut ==== latestSettings.orderTypeTakeOut
      settings.orderTypeDeliveryRestaurant ==== latestSettings.orderTypeDeliveryRestaurant
      settings.orderTypeInStore ==== latestSettings.orderTypeInStore
      settings.orderTypeInStorePickUp ==== latestSettings.orderTypeInStorePickUp
      settings.orderTypeDeliveryRetail ==== latestSettings.orderTypeDeliveryRetail
      settings.webStorefrontActive ==== latestSettings.webStorefrontActive
      settings.mobileStorefrontActive ==== latestSettings.mobileStorefrontActive
      settings.facebookStorefrontActive ==== latestSettings.facebookStorefrontActive
      settings.invoicesActive ==== latestSettings.invoicesActive
      settings.discountBelowCostActive ==== latestSettings.discountBelowCostActive
      settings.cashDrawerManagementActive ==== latestSettings.cashDrawerManagementActive
      settings.giftCardsActive ==== latestSettings.giftCardsActive
      settings.paymentTypeCreditCard ==== latestSettings.paymentTypeCreditCard
      settings.paymentTypeCash ==== latestSettings.paymentTypeCash
      settings.paymentTypeDebitCard ==== latestSettings.paymentTypeDebitCard
      settings.paymentTypeCheck ==== latestSettings.paymentTypeCheck
      settings.paymentTypeGiftCard ==== latestSettings.paymentTypeGiftCard
      settings.paymentTypeStoreCredit ==== latestSettings.paymentTypeStoreCredit
      settings.paymentTypeEbt ==== latestSettings.paymentTypeEbt
      settings.paymentTypeApplePay ==== latestSettings.paymentTypeApplePay
      settings.tipsHandling ==== latestSettings.tipsHandling
      settings.tipsOnDeviceEnabled ==== latestSettings.tipsOnDeviceEnabled
      settings.bypassSignatureAmount ==== latestSettings.bypassSignatureAmount
      settings.onlineStorefrontEnabled ==== latestSettings.onlineStorefrontEnabled
      settings.maxDrivingDistanceInMeters ==== latestSettings.maxDrivingDistanceInMeters
    }
  }

  "POST /v1/locations.create?location_id=$" in {
    "if request has valid token" in {
      "create location for restaurant and return 201" in new LocationsCreateFSpecContext {
        lazy val businessType = BusinessType.Restaurant
        override lazy val merchant = Factory.merchant(businessType = Some(businessType)).create

        val owner = Factory.user(merchant, isOwner = Some(true), active = Some(true)).create
        val availabilities = Seq(Availability(LocalTime.of(12, 34, 0), LocalTime.of(21, 43, 35)))
        val openingHours = Map(Weekdays.Monday -> availabilities, Weekdays.Tuesday -> availabilities)

        val locationCreation = random[LocationCreation].copy(openingHours = openingHours, email = Some(randomEmail))

        Post(s"/v1/locations.create?location_id=$newLocationId", locationCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val locationResponse = responseAs[ApiResponse[LocationEntity]].data
          assertCreation(locationCreation, locationResponse.id)
          assertResponseById(locationResponse, locationResponse.id)
          assertNextOrderNumber(newLocationId, locationCreation.initialOrderNumber)

          val userLocations = userLocationDao.findByLocationId(newLocationId).await
          userLocations.map(_.userId) should containTheSameElementsAs(Seq(user.id, owner.id))

          assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupLocations)
          assertLocationSettingsCreated(newLocationId, businessType)
        }
      }

      "create location for retail and return 201" in new LocationsCreateFSpecContext {
        lazy val businessType = BusinessType.Retail
        override lazy val merchant = Factory.merchant(businessType = Some(businessType)).create
        val owner = Factory.user(merchant, isOwner = Some(true), active = Some(true)).create
        val availabilities = Seq(Availability(LocalTime.of(12, 34, 0), LocalTime.of(21, 43, 35)))
        val openingHours = Map(Weekdays.Monday -> availabilities, Weekdays.Tuesday -> availabilities)

        val locationCreation = random[LocationCreation].copy(openingHours = openingHours, email = Some(randomEmail))

        Post(s"/v1/locations.create?location_id=$newLocationId", locationCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val locationResponse = responseAs[ApiResponse[LocationEntity]].data
          assertCreation(locationCreation, locationResponse.id)
          assertResponseById(locationResponse, locationResponse.id)
          assertNextOrderNumber(newLocationId, locationCreation.initialOrderNumber)

          val userLocations = userLocationDao.findByLocationId(newLocationId).await
          userLocations.map(_.userId) should containTheSameElementsAs(Seq(user.id, owner.id))

          assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupLocations)
          assertLocationSettingsCreated(newLocationId, businessType)
        }
      }

      "create location and return 201 copying setting for latest location" in new LocationsCreateFSpecContext {
        val owner = Factory.user(merchant, isOwner = Some(true), active = Some(true)).create
        val availabilities = Seq(Availability(LocalTime.of(12, 34, 0), LocalTime.of(21, 43, 35)))
        val openingHours = Map(Weekdays.Monday -> availabilities, Weekdays.Tuesday -> availabilities)

        val latestSettings = Factory.locationSettings(rome).create

        val locationCreation = random[LocationCreation].copy(openingHours = openingHours, email = Some(randomEmail))

        Post(s"/v1/locations.create?location_id=$newLocationId", locationCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val locationResponse = responseAs[ApiResponse[LocationEntity]].data
          assertCreation(locationCreation, locationResponse.id)
          assertResponseById(locationResponse, locationResponse.id)
          assertNextOrderNumber(newLocationId, locationCreation.initialOrderNumber)

          val userLocations = userLocationDao.findByLocationId(newLocationId).await
          userLocations.map(_.userId) should containTheSameElementsAs(Seq(user.id, owner.id))

          assertSetupStepCompleted(merchant, MerchantSetupSteps.SetupLocations)
          assertLocationSettingsCopiedFromPreviousLocation(newLocationId, latestSettings)
        }
      }

      "if location email is invalid" should {
        "return 400" in new LocationsCreateFSpecContext {
          val creation = random[LocationCreation].copy(email = Some("yadda"))

          Post(s"/v1/locations.create?location_id=$newLocationId", creation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }
    }
  }
}
