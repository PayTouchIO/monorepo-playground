package io.paytouch.core.resources.discounts

import java.time.LocalTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.DiscountType
import io.paytouch.core.entities.{ Discount => DiscountEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

import scala.util.Random

class DiscountsCreateFSpec extends DiscountsFSpec {

  abstract class DiscountsCreateFSpecContext extends DiscountResourceFSpecContext {

    def randomPercentageDiscountCreation =
      random[DiscountCreation].copy(`type` = DiscountType.Percentage)

    def randomNonPercentageDiscountCreation = {
      val discountType = {
        val seq = DiscountType.values.filter(_ != DiscountType.Percentage)
        seq(Random.nextInt(seq.size))
      }
      random[DiscountCreation].copy(`type` = discountType)
    }
  }

  "POST /v1/discount.create?discount_id=$" in {
    "if request has valid token" in {

      "create percentage discount and return 201" in new DiscountsCreateFSpecContext {
        val availabilities = {
          val times = Seq(Availability(LocalTime.of(12, 34, 56), LocalTime.of(21, 43, 35)))
          Map(Weekdays.Monday -> times, Weekdays.Tuesday -> times)
        }

        val newDiscountId = UUID.randomUUID
        val discountCreation =
          randomPercentageDiscountCreation.copy(requireManagerApproval = None, availabilityHours = Some(availabilities))

        Post(s"/v1/discounts.create?discount_id=$newDiscountId", discountCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val creationResponse = responseAs[ApiResponse[DiscountEntity]]
          val discountId = creationResponse.data.id

          val discountDb = discountDao.findById(discountId).await.head
          assertResponse(discountDb, creationResponse.data)
          assertUpdate(discountId, discountCreation.asUpdate)
        }
      }
      "create non-percentage discount and return 201" in new DiscountsCreateFSpecContext {
        val availabilities = {
          val times = Seq(Availability(LocalTime.of(12, 34, 56), LocalTime.of(21, 43, 35)))
          Map(Weekdays.Monday -> times, Weekdays.Tuesday -> times)
        }
        val newDiscountId = UUID.randomUUID
        val discountCreation = randomNonPercentageDiscountCreation.copy(
          requireManagerApproval = None,
          availabilityHours = Some(availabilities),
        )

        Post(s"/v1/discounts.create?discount_id=$newDiscountId", discountCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val creationResponse = responseAs[ApiResponse[DiscountEntity]]
          val discountId = creationResponse.data.id

          val discountDb = discountDao.findById(discountId).await.head
          assertResponse(discountDb, creationResponse.data)
          assertUpdate(discountId, discountCreation.asUpdate)
        }
      }

      "ignore currency when discount is a percentage" in new DiscountsCreateFSpecContext {
        val newDiscountId = UUID.randomUUID
        val discountCreation = random[DiscountCreation].copy(
          `type` = DiscountType.Percentage,
          requireManagerApproval = None,
          availabilityHours = None,
        )
        Post(s"/v1/discounts.create?discount_id=$newDiscountId", discountCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val creationResponse = responseAs[ApiResponse[DiscountEntity]]
          val discountId = creationResponse.data.id

          val discountDb = discountDao.findById(discountId).await.head
          assertResponse(discountDb, creationResponse.data)
          assertUpdate(discountId, discountCreation.asUpdate)
        }
      }

      "reject request if any location id does not exist of does not belong to merchant" in new DiscountsCreateFSpecContext {
        val competitor = Factory.merchant.create
        val competitorLocation = Factory.location(competitor).create

        val competitorLocationOverrides = Map(competitorLocation.id -> None)
        val newDiscountId = UUID.randomUUID
        val discountCreation =
          randomNonPercentageDiscountCreation.copy(
            requireManagerApproval = None,
            locationOverrides = competitorLocationOverrides,
          )

        Post(s"/v1/discounts.create?discount_id=$newDiscountId", discountCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)

          discountDao.findById(newDiscountId).await ==== None
          itemLocationDao.findByItemId(newDiscountId).await ==== Seq.empty
        }
      }
    }
  }
}
