package io.paytouch.core.resources.producthistory

import java.time.ZonedDateTime

import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.ProductQuantityHistoryRecord
import io.paytouch.core.entities._
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ProductQuantityHistoryListFSpec extends FSpec {
  abstract class ProductQuantityHistoryListFSpecContext extends FSpecContext with JsonSupport with Fixtures {
    def assertResponse(record: ProductQuantityHistoryRecord, entity: ProductQuantityHistory) = {
      record.id ==== entity.id
      record.prevQuantityAmount ==== entity.prevQuantity
      record.newQuantityAmount ==== entity.newQuantity
      record.newStockValueAmount ==== entity.newStockValue.amount
      record.reason ==== entity.reason
      record.locationId ==== entity.location.id
      record.userId ==== entity.user.map(_.id)
      record.notes ==== entity.notes
    }
  }

  "GET /v1/products.list_quantity_changes?product_id=<product" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return a paginated list of all quantity changes sorted by received at in descending order" in new ProductQuantityHistoryListFSpecContext {
          val order = Factory.order(merchant).create

          val otherProduct = Factory.simpleProduct(merchant).create
          val simpleChange = Factory.productQuantityHistory(product, rome, date = Some(UtcTime.now)).create
          val changeWithOrder = Factory
            .productQuantityHistory(product, rome, order = Some(order), date = Some(UtcTime.now.plusDays(1)))
            .create
          val changeWithUser = Factory
            .productQuantityHistory(product, rome, user = Some(user), date = Some(UtcTime.now.plusDays(2)))
            .create

          val changeOtherProduct = Factory.productQuantityHistory(otherProduct, rome).create

          Get(s"/v1/products.list_quantity_changes?product_id=${product.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val changes = responseAs[PaginatedApiResponse[Seq[ProductQuantityHistory]]]
            changes.data.map(_.id) ==== Seq(changeWithUser.id, changeWithOrder.id, simpleChange.id)
            assertResponse(simpleChange, changes.data.find(_.id == simpleChange.id).get)
            assertResponse(changeWithOrder, changes.data.find(_.id == changeWithOrder.id).get)
            assertResponse(changeWithUser, changes.data.find(_.id == changeWithUser.id).get)
          }
        }
      }

      "filtered by from date-time" should {
        "return a paginated list of all quantity changes sorted by order at in descending order and filtered by a from date-time" in new ProductQuantityHistoryListFSpecContext {
          val today = ZonedDateTime.parse("2015-12-15T00:00:00Z")

          val changeToday = Factory.productQuantityHistory(product, rome, date = Some(today)).create
          val changeYesterday =
            Factory.productQuantityHistory(product, rome, date = Some(today.minusDays(1))).create
          val changeLastMonth =
            Factory.productQuantityHistory(product, rome, date = Some(today.minusDays(30))).create

          val threeDaysAgo = UtcTime.now.minusDays(3)

          Get(s"/v1/products.list_quantity_changes?product_id=${product.id}&from=2015-12-01T00:00:00")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val changes = responseAs[PaginatedApiResponse[Seq[ProductQuantityHistory]]]
            changes.data.map(_.id) ==== Seq(changeToday.id, changeYesterday.id)
            assertResponse(changeToday, changes.data.find(_.id == changeToday.id).get)
            assertResponse(changeYesterday, changes.data.find(_.id == changeYesterday.id).get)
          }
        }
      }

      "filtered by to date-time" should {
        "return a paginated list of all quantity changes sorted by order at in descending order and filtered by a to date-time" in new ProductQuantityHistoryListFSpecContext {
          val today = ZonedDateTime.parse("2015-12-15T00:00:00Z")

          val changeToday = Factory.productQuantityHistory(product, rome, date = Some(today)).create
          val changeLastWeek =
            Factory.productQuantityHistory(product, rome, date = Some(today.minusDays(7))).create
          val changeLastMonth =
            Factory.productQuantityHistory(product, rome, date = Some(today.minusDays(30))).create

          val threeDaysAgo = UtcTime.now.minusDays(3)

          Get(s"/v1/products.list_quantity_changes?product_id=${product.id}&to=2015-12-13T00:00:00")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val changes = responseAs[PaginatedApiResponse[Seq[ProductQuantityHistory]]]
            changes.data.map(_.id) ==== Seq(changeLastWeek.id, changeLastMonth.id)
            assertResponse(changeLastMonth, changes.data.find(_.id == changeLastMonth.id).get)
            assertResponse(changeLastWeek, changes.data.find(_.id == changeLastWeek.id).get)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ProductQuantityHistoryListFSpecContext {
        Get(s"/v1/products.list_quantity_changes?product_id=${product.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

  trait Fixtures extends MultipleLocationFixtures {
    val product = Factory.simpleProduct(merchant).create
  }
}
