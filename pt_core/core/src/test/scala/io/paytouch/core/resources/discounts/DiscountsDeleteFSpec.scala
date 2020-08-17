package io.paytouch.core.resources.discounts

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class DiscountsDeleteFSpec extends DiscountsFSpec {

  abstract class DiscountDeleteResourceFSpecContext extends DiscountResourceFSpecContext {
    val discountLocationDao = daos.discountLocationDao
    val discountAvailabilityDao = daos.discountAvailabilityDao

    def assertDiscountDeleted(id: UUID) = {
      discountDao.findById(id).await should beNone
      discountAvailabilityDao.findByItemId(id).await should beEmpty
      discountLocationDao.findByItemId(id).await should beEmpty
    }
    def assertDiscountNotDeleted(id: UUID) = discountDao.findById(id).await should beSome
  }

  "POST /v1/discounts.delete" in {

    "if request has valid token" in {
      "if discount doesn't exist" should {
        "do nothing and return 204" in new DiscountDeleteResourceFSpecContext {
          val nonExistingDiscountId = UUID.randomUUID

          Post(s"/v1/discounts.delete", Ids(ids = Seq(nonExistingDiscountId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertDiscountDeleted(nonExistingDiscountId)
          }
        }
      }

      "if discount belongs to the merchant" should {
        "delete the discount and the related discount locations and return 204" in new DiscountDeleteResourceFSpecContext {
          val discount = Factory.discount(merchant).create
          val discountAvailability = Factory.discountAvailability(discount, Seq.empty).create
          val discountLocation = Factory.discountLocation(discount, rome).create
          val order = Factory.order(merchant).create
          val orderItem = Factory.orderItem(order).create
          val ticketDiscount = Factory.orderDiscount(order, discount).create
          val orderItemDiscount = Factory.orderItemDiscount(orderItem, discount).create

          Post(s"/v1/discounts.delete", Ids(ids = Seq(discount.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertDiscountDeleted(discount.id)
          }
        }
      }

      "if discount belongs to a different merchant" should {
        "do not delete the discount and return 204" in new DiscountDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorDiscount = Factory.discount(competitor).create

          Post(s"/v1/discounts.delete", Ids(ids = Seq(competitorDiscount.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertDiscountNotDeleted(competitorDiscount.id)
          }
        }
      }
    }
  }
}
