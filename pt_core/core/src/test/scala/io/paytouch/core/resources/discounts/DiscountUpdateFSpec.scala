package io.paytouch.core.resources.discounts

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ Discount => DiscountEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class DiscountUpdateFSpec extends DiscountsFSpec {

  "POST /v1/discount.update?discount_id=$" in {
    "if request has valid token" in {
      "if discount belongs to current merchant" should {
        "update the discount" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant).create

          val discountUpdate = random[DiscountUpdate].copy(`type` = None, availabilityHours = None)

          Post(s"/v1/discounts.update?discount_id=${discount.id}", discountUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val updateResponse = responseAs[ApiResponse[DiscountEntity]]
            val discountDb = discountDao.findById(discount.id).await.head

            assertResponse(discountDb, updateResponse.data)
            assertUpdate(discount.id, discountUpdate)
          }
        }

        "update the discount with location overrides" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant).create

          val discountLocationRome = Factory.discountLocation(discount, rome, active = Some(false)).create
          val discountLocationLondon = Factory.discountLocation(discount, london).create

          val locationOverridesUpdate = Map(
            rome.id -> Some(ItemLocationUpdate(active = Some(false))),
            london.id -> None,
          )

          val availabilityHoursUpdate = buildAvailability

          val discountUpdate = random[DiscountUpdate].copy(
            `type` = None,
            locationOverrides = locationOverridesUpdate,
            availabilityHours = Some(availabilityHoursUpdate),
          )

          Post(s"/v1/discounts.update?discount_id=${discount.id}", discountUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val updateResponse = responseAs[ApiResponse[DiscountEntity]]
            val discountDb = discountDao.findById(discount.id).await.head

            assertResponse(discountDb, updateResponse.data)
            assertUpdate(discount.id, discountUpdate)
          }
        }
        "reject request if any location id does not exist or does not belong to merchant" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant).create

          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create

          val competitorLocationOverrides = Map(competitorLocation.id -> None)

          val discountUpdate =
            random[DiscountUpdate].copy(`type` = None, locationOverrides = competitorLocationOverrides)

          Post(s"/v1/discounts.update?discount_id=${discount.id}", discountUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            discountDao.findById(discount.id).await.get ==== discount
          }
        }
      }
      "if discount doesn't belong to current merchant" should {
        "not update discount and return 404" in new DiscountResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDiscount = Factory.discount(competitor).create
          val discountUpdate = random[DiscountUpdate]

          Post(s"/v1/discounts.update?discount_id=${competitorDiscount.id}", discountUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            discountDao.findById(competitorDiscount.id).await.get ==== competitorDiscount
          }
        }
      }
    }
  }
}
