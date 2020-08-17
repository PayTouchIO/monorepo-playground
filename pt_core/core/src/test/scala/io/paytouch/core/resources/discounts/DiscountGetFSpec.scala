package io.paytouch.core.resources.discounts

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ Discount => DiscountEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class DiscountGetFSpec extends DiscountsFSpec {

  "GET /v1/discounts.get?discount_id=<discount-id>" in {
    "if request has valid token" in {

      "if the discount belongs to the merchant" should {
        "return a discount" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant, locations = Seq(rome)).create

          Get(s"/v1/discounts.get?discount_id=${discount.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val discountResponse = responseAs[ApiResponse[DiscountEntity]]
            assertResponse(discount, discountResponse.data)
          }
        }

        "return a discount with locations" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant, locations = Seq(london, rome)).create

          Get(s"/v1/discounts.get?discount_id=${discount.id}&expand[]=locations")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val discountResponse = responseAs[ApiResponse[DiscountEntity]]
            assertResponse(discount, discountResponse.data, Some(Seq(london, rome)))
          }
        }

        "return a discount with location availabilities" in new DiscountResourceFSpecContext {
          val discount = Factory.discount(merchant).create
          val discountLondon = Factory.discountLocation(discount, london, active = Some(true)).create
          val discountRome = Factory.discountLocation(discount, rome, active = Some(false)).create

          val discountAvailability =
            Factory.discountAvailability(discount, Seq(Weekdays.Monday, Weekdays.Tuesday)).create

          val expectedAvailabilityMap = Map(
            Weekdays.Monday -> Seq(Availability(discountAvailability.start, discountAvailability.end)),
            Weekdays.Tuesday -> Seq(Availability(discountAvailability.start, discountAvailability.end)),
          )

          Get(s"/v1/discounts.get?discount_id=${discount.id}&expand[]=availabilities")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val discountResponse = responseAs[ApiResponse[DiscountEntity]].data
            assertResponse(discount, discountResponse)
            discountResponse.availabilityHours ==== Some(expectedAvailabilityMap)
          }
        }
      }

      "if the discount does not belong to the merchant" should {
        "return 404" in new DiscountResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDiscount = Factory.discount(competitor).create

          Get(s"/v1/discounts.get?discount_id=${competitorDiscount.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
