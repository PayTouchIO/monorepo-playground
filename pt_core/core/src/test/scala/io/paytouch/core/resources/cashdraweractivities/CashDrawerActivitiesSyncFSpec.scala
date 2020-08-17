package io.paytouch.core.resources.cashdraweractivities

import java.time.ZoneId
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ CashDrawerActivity => CashDrawerActivityEntity, _ }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CashDrawerActivitiesSyncFSpec extends CashDrawerActivitiesFSpec {

  abstract class CashDrawersSyncFSpecContext extends CashDrawerActivityResourceFSpecContext {

    def assertUpsertion(
        id: UUID,
        upsertion: CashDrawerActivityUpsertion,
        withEmptyUserId: Boolean = false,
      ) = {
      val record = cashDrawerActivityDao.findById(id).await.get
      record.id ==== id
      record.userId ==== (if (withEmptyUserId) None else Some(upsertion.userId))
      record.cashDrawerId ==== Some(upsertion.cashDrawerId)
      upsertion.`type` ==== record.`type`
      if (upsertion.startingCashAmount.isDefined) upsertion.startingCashAmount ==== record.startingCashAmount
      if (upsertion.endingCashAmount.isDefined) upsertion.endingCashAmount ==== record.endingCashAmount
      if (upsertion.payInAmount.isDefined) upsertion.payInAmount ==== record.payInAmount
      if (upsertion.payOutAmount.isDefined) upsertion.payOutAmount ==== record.payOutAmount
      if (upsertion.tipInAmount.isDefined) upsertion.tipInAmount ==== record.tipInAmount
      if (upsertion.tipOutAmount.isDefined) upsertion.tipOutAmount ==== record.tipOutAmount
      upsertion.currentBalanceAmount ==== record.currentBalanceAmount
      upsertion.tipForUserId ==== record.tipForUserId
      upsertion.timestamp.withZoneSameLocal(ZoneId.of("Z")) ==== record.timestamp
      if (upsertion.notes.isDefined) upsertion.notes ==== record.notes
    }
  }

  "POST /v1/cash_drawer_activities.sync?cash_drawer_activity_id=$" in {
    "if request has valid token" in {

      "if the cash drawer activity doesn't exist" should {
        "creates the cash drawer activity" in new CashDrawersSyncFSpecContext {
          val anotherUser = Factory.user(merchant).create
          val cashDrawer = Factory.cashDrawer(anotherUser, rome).create
          val cashDrawerActivityId = UUID.randomUUID

          val upsertion =
            random[CashDrawerActivityUpsertion].copy(
              userId = anotherUser.id,
              cashDrawerId = cashDrawer.id,
              tipForUserId = Some(user.id),
            )

          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=$cashDrawerActivityId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivityId ==== entity.id
            assertUpsertion(entity.id, upsertion)
            assertResponseById(entity.id, entity)
          }
        }
      }

      "if the cash drawer activity already exists" should {
        "updates the cash drawer" in new CashDrawersSyncFSpecContext {
          val anotherUser = Factory.user(merchant).create
          val cashDrawer = Factory.cashDrawer(anotherUser, rome).create
          val cashDrawerActivity = Factory.cashDrawerActivity(cashDrawer).create

          val upsertion =
            random[CashDrawerActivityUpsertion].copy(userId = anotherUser.id, cashDrawerId = cashDrawer.id)

          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=${cashDrawerActivity.id}", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivity.id ==== entity.id
            assertUpsertion(entity.id, upsertion)
            assertResponseById(entity.id, entity)
          }
        }
      }

      "even if the id is not available" should {
        "it should save the cash drawer activity" in new CashDrawersSyncFSpecContext {
          val competitor = Factory.merchant.create
          val userCompetitor = Factory.user(competitor).create
          val locationCompetitor = Factory.location(competitor).create
          val cashDrawerCompetitor = Factory.cashDrawer(userCompetitor, locationCompetitor).create
          val cashDrawer = Factory.cashDrawer(user, rome).create
          val cashDrawerActivityCompetitor = Factory.cashDrawerActivity(cashDrawerCompetitor).create

          val upsertion = random[CashDrawerActivityUpsertion].copy(userId = user.id, cashDrawerId = cashDrawer.id)
          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=${cashDrawerActivityCompetitor.id}", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivityCompetitor.id !=== entity.id
            assertUpsertion(entity.id, upsertion)
            assertResponseById(entity.id, entity)
          }
        }
      }

      "even if the cash drawer id id does not exist" should {
        "it should save the cash drawer activity" in new CashDrawersSyncFSpecContext {
          val anotherUser = Factory.user(merchant).create
          val cashDrawerActivityId = UUID.randomUUID

          val upsertion = random[CashDrawerActivityUpsertion].copy(userId = anotherUser.id)

          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=$cashDrawerActivityId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivityId ==== entity.id
            assertUpsertion(entity.id, upsertion)
            assertResponseById(entity.id, entity)
          }
        }
      }

      "even if the user id does not exist" should {
        "it should save the cash drawer activity" in new CashDrawersSyncFSpecContext {
          val cashDrawer = Factory.cashDrawer(user, rome).create
          val cashDrawerActivityId = UUID.randomUUID

          val upsertion = random[CashDrawerActivityUpsertion].copy(cashDrawerId = cashDrawer.id)

          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=$cashDrawerActivityId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivityId ==== entity.id
            assertUpsertion(entity.id, upsertion, withEmptyUserId = true)
            assertResponseById(entity.id, entity)
          }
        }
      }

      "even if all the ids are random" should {
        "it should save the cash drawer activity" in new CashDrawersSyncFSpecContext {
          val cashDrawerActivityId = UUID.randomUUID

          val upsertion = random[CashDrawerActivityUpsertion]

          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=$cashDrawerActivityId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivityId ==== entity.id
            assertUpsertion(entity.id, upsertion, withEmptyUserId = true)
            assertResponseById(entity.id, entity)
          }
        }
      }

      "even if a user has been soft-deleted" should {
        "it should save the cash drawer activity" in new CashDrawersSyncFSpecContext {
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now)).create
          val cashDrawer = Factory.cashDrawer(deletedUser, rome).create
          val cashDrawerActivityId = UUID.randomUUID

          val upsertion =
            random[CashDrawerActivityUpsertion].copy(userId = deletedUser.id, cashDrawerId = cashDrawer.id)

          Post(s"/v1/cash_drawer_activities.sync?cash_drawer_activity_id=$cashDrawerActivityId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val entity = responseAs[ApiResponse[CashDrawerActivityEntity]].data
            cashDrawerActivityId ==== entity.id
            assertUpsertion(entity.id, upsertion)
            assertResponseById(entity.id, entity)
          }
        }
      }
    }
  }
}
