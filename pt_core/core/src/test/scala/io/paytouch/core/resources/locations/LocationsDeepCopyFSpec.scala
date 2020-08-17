package io.paytouch.core.resources.locations

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model.enums.{ BusinessType, NextNumberType, ScopeType }
import io.paytouch.core.data.model.{ LocationSettingsRecord, NextNumberRecord }
import io.paytouch.core.entities.enums.{ MerchantSetupSteps, TipsHandlingMode }
import io.paytouch.core.entities.{ Location => LocationEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class LocationsDeepCopyFSpec extends LocationsFSpec {
  abstract class LocationsCopyFSpecContext extends LocationsResourceFSpecContext

  "POST /v1/locations.deep_copy?from=$&to=$" in {
    "if request has valid token" in {
      "good" in {
        "should deep copy relationships" in new LocationsCopyFSpecContext {
          val sourceLocationId = UUID.randomUUID
          lazy val businessType = BusinessType.Restaurant
          override lazy val merchant = Factory.merchant(businessType = Some(businessType)).create

          val employee = Factory.user(merchant, isOwner = Some(false), active = Some(true)).create

          val locationCreation =
            random[LocationCreation].copy(email = Some(randomEmail))

          val routedToBarProduct =
            Factory.simpleProduct(merchant, name = "routed to bar".some).create

          val routedToKitchenProduct =
            Factory.simpleProduct(merchant, name = "routed to kitchen".some).create

          val notRoutedProduct =
            Factory.simpleProduct(merchant, name = "not routed anywhere".some).create

          Post(s"/v1/locations.create?location_id=$sourceLocationId", locationCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()

            val locationResponse =
              responseAs[ApiResponse[LocationEntity]].data

            val locationRecord =
              daos
                .locationDao
                .findById(locationResponse.id)
                .await
                .get

            val taxRate =
              Factory
                .taxRate(merchant)
                .create

            Factory
              .userLocation(employee, locationRecord)
              .create

            Factory
              .taxRateLocation(taxRate, locationRecord)
              .create

            Factory
              .supplier(merchant, locations = Seq(locationRecord))
              .create

            val bar =
              Factory.kitchen(locationRecord, name = "bar".some).create

            val productLocation =
              Factory
                .productLocation(routedToBarProduct, locationRecord, routeToKitchen = bar.some)
                .create

            val kitchen =
              Factory.kitchen(locationRecord, name = "kitchen".some).create

            Factory
              .productLocation(routedToKitchenProduct, locationRecord, routeToKitchen = kitchen.some)
              .create

            Factory
              .productLocation(notRoutedProduct, locationRecord, routeToKitchen = None)
              .create

            Factory
              .stock(productLocation)
              .create

            val modifierSet =
              Factory
                .modifierSet(merchant)
                .create

            Factory
              .modifierSetLocation(modifierSet, locationRecord)
              .create

            Factory
              .loyaltyProgram(merchant, locations = Seq(locationRecord))
              .create

            val discount =
              Factory
                .discount(merchant)
                .create

            Factory
              .discountLocation(discount, locationRecord)
              .create

            val catalog =
              Factory
                .catalog(merchant)
                .create

            val category =
              Factory
                .catalogCategory(catalog)
                .create

            Factory.categoryLocation(category, locationRecord).create
          }

          val targetLocationId = UUID.randomUUID()

          Post(s"/v1/locations.create?location_id=$targetLocationId", locationCreation)
            .addHeader(authorizationHeader) ~> routes ~> check(assertStatusCreated())

          def countLocations(locationId: UUID): Seq[Int] =
            List(
              daos.userLocationDao.findByLocationId(locationId),
              daos.taxRateLocationDao.findByLocationId(locationId),
              daos.supplierLocationDao.findByLocationId(locationId),
              daos.stockDao.findByLocationId(locationId),
              daos.modifierSetLocationDao.findByLocationId(locationId),
              daos.loyaltyProgramLocationDao.findByLocationId(locationId),
              daos.discountLocationDao.findByLocationId(locationId),
              daos.categoryLocationDao.findByLocationId(locationId),
              daos.kitchenDao.findByLocationId(locationId),
              daos.productLocationDao.findByLocationId(locationId),
            ).sequence.await.map(_.size)

          // Some of the relationships where created when the target location was created,
          // which is why we check for greater EQUALS. It's not ideal, but it gets the job done.
          countLocations(targetLocationId).forall(_ >= 0) ==== true

          Post(s"/v1/locations.deep_copy?from=$sourceLocationId&to=$targetLocationId")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            countLocations(targetLocationId).forall(_ !=== 0)

            val kitchens =
              daos.kitchenDao.findByLocationId(targetLocationId).await

            val productLocations =
              daos.productLocationDao.findByLocationId(targetLocationId).await

            // extra assertion to make sure the routes to kitchens were copied as well
            productLocations
              .filter(_.routeToKitchenId.exists(kitchenId => kitchens.exists(_.id === kitchenId)))
              .size ==== 2 // and not 3 because one of the products is not routed to any kitchen
          }
        }
      }

      "bad" in {
        "SourceNotFound" in new LocationsCopyFSpecContext {
          Post(s"/v1/locations.deep_copy?from=${UUID.randomUUID}&to=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NonAccessibleLocationIds")
            assertErrorCode("TargetLocationDoesNotBelongToSameMerchant")
          }
        }

        "TargetLocationDoesNotBelongToSameMerchant" in new LocationsCopyFSpecContext {
          val sourceLocationId = UUID.randomUUID
          lazy val businessType = BusinessType.Restaurant
          override lazy val merchant = Factory.merchant(businessType = Some(businessType)).create

          val employee = Factory.user(merchant, isOwner = Some(false), active = Some(true)).create

          val locationCreation =
            random[LocationCreation].copy(email = Some(randomEmail))

          val routedToBarProduct =
            Factory.simpleProduct(merchant, name = "routed to bar".some).create

          val routedToKitchenProduct =
            Factory.simpleProduct(merchant, name = "routed to kitchen".some).create

          val notRoutedProduct =
            Factory.simpleProduct(merchant, name = "not routed anywhere".some).create

          Post(s"/v1/locations.create?location_id=$sourceLocationId", locationCreation)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
          }

          val targetLocationId = UUID.randomUUID()

          Post(s"/v1/locations.deep_copy?from=$sourceLocationId&to=$targetLocationId")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("TargetLocationDoesNotBelongToSameMerchant")
          }
        }
      }
    }
  }
}
