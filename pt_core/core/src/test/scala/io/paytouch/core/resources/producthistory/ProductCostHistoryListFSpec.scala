package io.paytouch.core.resources.producthistory

import java.time.ZonedDateTime

import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.core.data.model.ProductCostHistoryRecord
import io.paytouch.core.entities._
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ProductCostHistoryListFSpec extends FSpec {
  abstract class ProductCostHistoryListFSpecContext extends FSpecContext with JsonSupport with Fixtures {
    def assertResponse(record: ProductCostHistoryRecord, entity: ProductCostHistory) = {
      record.id ==== entity.id
      record.prevCostAmount ==== entity.prevCost.amount
      record.newCostAmount ==== entity.newCost.amount
      (record.newCostAmount - record.prevCostAmount) ==== entity.costChange.amount
      currency ==== entity.prevCost.currency
      currency ==== entity.newCost.currency
      currency ==== entity.costChange.currency
      record.reason ==== entity.reason
      record.locationId ==== entity.location.id
      record.userId ==== entity.user.id
      record.notes ==== entity.notes
    }
  }

  "GET /v1/products.list_cost_changes?product_id=<product" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return a paginated list of all cost changes sorted by received at in descending order" in new ProductCostHistoryListFSpecContext {
          val order = Factory.order(merchant).create

          val otherProduct = Factory.simpleProduct(merchant).create
          val change1 = Factory.productCostHistory(product, rome, user, date = Some(UtcTime.now)).create
          val change2 =
            Factory.productCostHistory(product, rome, user, date = Some(UtcTime.now.plusDays(2))).create
          val change3 =
            Factory.productCostHistory(product, rome, user, date = Some(UtcTime.now.plusDays(3))).create

          val changeOtherProduct = Factory.productCostHistory(otherProduct, rome, user).create

          Get(s"/v1/products.list_cost_changes?product_id=${product.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val changes = responseAs[PaginatedApiResponse[Seq[ProductCostHistory]]]
            changes.data.map(_.id) ==== Seq(change3.id, change2.id, change1.id)
            assertResponse(change1, changes.data.find(_.id == change1.id).get)
            assertResponse(change2, changes.data.find(_.id == change2.id).get)
            assertResponse(change3, changes.data.find(_.id == change3.id).get)
          }
        }
      }

      "filtered by from date-time" should {
        "return a paginated list of all cost changes sorted by order at in descending order and filtered by a from date-time" in new ProductCostHistoryListFSpecContext {
          val today = ZonedDateTime.parse("2015-12-15T00:00:00Z")

          val changeToday = Factory.productCostHistory(product, rome, user, date = Some(today)).create
          val changeYesterday =
            Factory.productCostHistory(product, rome, user, date = Some(today.minusDays(1))).create
          val changeLastMonth =
            Factory.productCostHistory(product, rome, user, date = Some(today.minusDays(30))).create

          val threeDaysAgo = UtcTime.now.minusDays(3)

          Get(s"/v1/products.list_cost_changes?product_id=${product.id}&from=2015-12-01T00:00:00")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val changes = responseAs[PaginatedApiResponse[Seq[ProductCostHistory]]]
            changes.data.map(_.id) ==== Seq(changeToday.id, changeYesterday.id)
            assertResponse(changeToday, changes.data.find(_.id == changeToday.id).get)
            assertResponse(changeYesterday, changes.data.find(_.id == changeYesterday.id).get)
          }
        }
      }

      "filtered by to date-time" should {
        "return a paginated list of all cost changes sorted by order at in descending order and filtered by a to date-time" in new ProductCostHistoryListFSpecContext {
          val today = ZonedDateTime.parse("2015-12-15T00:00:00Z")

          val changeToday = Factory.productCostHistory(product, rome, user, date = Some(today)).create
          val changeLastWeek =
            Factory.productCostHistory(product, rome, user, date = Some(today.minusDays(7))).create
          val changeLastMonth =
            Factory.productCostHistory(product, rome, user, date = Some(today.minusDays(30))).create

          val threeDaysAgo = UtcTime.now.minusDays(3)

          Get(s"/v1/products.list_cost_changes?product_id=${product.id}&to=2015-12-13T00:00:00")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val changes = responseAs[PaginatedApiResponse[Seq[ProductCostHistory]]]
            changes.data.map(_.id) ==== Seq(changeLastWeek.id, changeLastMonth.id)
            assertResponse(changeLastMonth, changes.data.find(_.id == changeLastMonth.id).get)
            assertResponse(changeLastWeek, changes.data.find(_.id == changeLastWeek.id).get)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new ProductCostHistoryListFSpecContext {
        Get(s"/v1/products.list_cost_changes?product_id=${product.id}")
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
