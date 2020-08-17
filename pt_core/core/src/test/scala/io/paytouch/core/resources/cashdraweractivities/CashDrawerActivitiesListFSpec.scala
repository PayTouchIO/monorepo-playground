package io.paytouch.core.resources.cashdraweractivities

import java.time.ZonedDateTime

import io.paytouch.core.entities.{ CashDrawerActivity => CashDrawerActivityEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CashDrawerActivitiesListFSpec extends CashDrawerActivitiesFSpec {

  "GET /v1/cash_drawer_activities.list" in {
    "if request has valid token" in {

      "with cash_drawer_id parameter" should {
        "return a paginated list of all cash drawer activities for a specific cash_drawer" in new CashDrawerActivityResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val userInNewYork = Factory.user(merchant, locations = Seq(newYork)).create

          val cashDrawer = Factory.cashDrawer(user, rome).create
          val activity1 = Factory.cashDrawerActivity(cashDrawer).create
          val activity2 = Factory.cashDrawerActivity(cashDrawer).create
          val activity3 = Factory.cashDrawerActivity(cashDrawer).create
          val otherCashDrawer = Factory.cashDrawer(user, rome).create
          val otherActivity = Factory.cashDrawerActivity(otherCashDrawer).create

          Get(s"/v1/cash_drawer_activities.list?cash_drawer_id=${cashDrawer.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val activities = responseAs[PaginatedApiResponse[Seq[CashDrawerActivityEntity]]]
            activities.data.map(_.id) ==== Seq(activity1.id, activity2.id, activity3.id)
            assertResponse(activity1, activities.data.find(_.id == activity1.id).get)
            assertResponse(activity2, activities.data.find(_.id == activity2.id).get)
            assertResponse(activity3, activities.data.find(_.id == activity3.id).get)
          }
        }
      }

      "with updated_since parameter" should {
        "return a paginated list of all cash drawers activities filtered by cash drawers activities updated since the given time" in new CashDrawerActivityResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val cashDrawer = Factory.cashDrawer(user, rome).create
          val activity1 = Factory.cashDrawerActivity(cashDrawer, overrideNow = Some(now.minusDays(1))).create
          val activity2 = Factory.cashDrawerActivity(cashDrawer, overrideNow = Some(now)).create
          val activity3 = Factory.cashDrawerActivity(cashDrawer, overrideNow = Some(now.plusDays(1))).create

          Get(s"/v1/cash_drawer_activities.list?cash_drawer_id=${cashDrawer.id}&updated_since=2015-12-03")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val activities = responseAs[PaginatedApiResponse[Seq[CashDrawerActivityEntity]]]
            activities.data.map(_.id) ==== Seq(activity2.id, activity3.id)
            activities.data.length ==== 2
            assertResponse(activity2, activities.data.find(_.id == activity2.id).get)
            assertResponse(activity3, activities.data.find(_.id == activity3.id).get)
          }
        }
      }
    }
  }

}
