package io.paytouch.core.resources.cashdrawers

import java.time.ZoneId
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, _ }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CashDrawersSyncFSpec extends CashDrawersFSpec {

  abstract class CashDrawersSyncFSpecContext extends CashDrawerResourceFSpecContext {
    val baseUpsertion = random[CashDrawerUpsertion].copy(appendActivities = None)

    def assertUpsertion(
        id: UUID,
        upsertion: CashDrawerUpsertion,
        withEmptyLocationId: Boolean = false,
        withEmptyUserId: Boolean = false,
      ) = {
      val record = cashDrawerDao.findById(id).await.get
      record.id ==== id
      record.locationId ==== (if (withEmptyLocationId) None else Some(upsertion.locationId))
      record.userId ==== (if (withEmptyUserId) None else Some(upsertion.userId))
      if (upsertion.startingCashAmount.isDefined)
        upsertion.startingCashAmount.toOption ==== Some(record.startingCashAmount)
      else record.startingCashAmount ==== BigDecimal(0)
      if (upsertion.employeeId.isDefined) upsertion.employeeId ==== record.employeeId
      if (upsertion.endingCashAmount.isDefined) upsertion.endingCashAmount ==== record.endingCashAmount
      if (upsertion.cashSalesAmount.isDefined) upsertion.cashSalesAmount ==== record.cashSalesAmount
      if (upsertion.cashRefundsAmount.isDefined) upsertion.cashRefundsAmount ==== record.cashRefundsAmount
      if (upsertion.paidInAndOutAmount.isDefined) upsertion.paidInAndOutAmount ==== record.paidInAndOutAmount
      if (upsertion.paidInAmount.isDefined) upsertion.paidInAmount ==== record.paidInAmount
      if (upsertion.paidOutAmount.isDefined) upsertion.paidOutAmount ==== record.paidOutAmount
      if (upsertion.tippedInAmount.isDefined) upsertion.tippedInAmount ==== record.tippedInAmount
      if (upsertion.tippedOutAmount.isDefined) upsertion.tippedOutAmount ==== record.tippedOutAmount
      if (upsertion.expectedAmount.isDefined) upsertion.expectedAmount ==== record.expectedAmount
      upsertion.status ==== record.status
      upsertion.startedAt.withZoneSameLocal(ZoneId.of("Z")) ==== record.startedAt
      if (upsertion.endedAt.isDefined)
        upsertion.endedAt.toOption.map(_.withZoneSameLocal(ZoneId.of("Z"))) ==== record.endedAt
      if (upsertion.printerMacAddress.isDefined) upsertion.printerMacAddress ==== record.printerMacAddress

      if (upsertion.appendActivities.isDefined) {
        val activityRecords = cashDrawerActivityDao.findAllByMerchantIdAndCashDrawerId(merchant.id, id).await
        activityRecords.map(_.id) must containAllOf(upsertion.appendActivities.get.map(_.id))
      }
    }
  }

  "POST /v1/cash_drawers.sync?cash_drawer_id=$" in {
    "if request has valid token" in {

      "if the cash drawer doesn't exist" should {
        "creates the cash drawer" in new CashDrawersSyncFSpecContext {
          val anotherUser = Factory.user(merchant).create
          val cashDrawerId = UUID.randomUUID

          val upsertion = baseUpsertion.copy(
            locationId = rome.id,
            userId = anotherUser.id,
            employeeId = genOptInt.instance.map(_ => user.id),
            appendActivities = Some(Seq(random[CashDrawerActivityUpsertion].copy(cashDrawerId = cashDrawerId))),
          )

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=$cashDrawerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerId ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "if the cash drawer already exists" should {
        "updates the cash drawer skipping existing activities" in new CashDrawersSyncFSpecContext {
          val anotherUser = Factory.user(merchant).create
          val cashDrawer = Factory.cashDrawer(anotherUser, rome).create
          val cashDrawerActivity = Factory.cashDrawerActivity(cashDrawer).create

          val upsertion =
            baseUpsertion.copy(
              locationId = london.id,
              userId = anotherUser.id,
              appendActivities = Some(
                Seq(random[CashDrawerActivityUpsertion].copy(id = cashDrawerActivity.id, cashDrawerId = cashDrawer.id)),
              ),
            )

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=${cashDrawer.id}", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawer.id ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "even if the id is not available" should {
        "it should save the cash drawer" in new CashDrawersSyncFSpecContext {
          val competitor = Factory.merchant.create
          val userCompetitor = Factory.user(competitor).create
          val locationCompetitor = Factory.location(competitor).create
          val cashDrawerCompetitor = Factory.cashDrawer(userCompetitor, locationCompetitor).create

          val upsertion = baseUpsertion.copy(locationId = london.id, userId = user.id)

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=${cashDrawerCompetitor.id}", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerCompetitor.id !=== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "even if the location id does not exist" should {
        "it should save the cash drawer" in new CashDrawersSyncFSpecContext {
          val anotherUser = Factory.user(merchant).create
          val cashDrawerId = UUID.randomUUID

          val upsertion = baseUpsertion.copy(userId = anotherUser.id)

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=$cashDrawerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerId ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion, withEmptyLocationId = true)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "even if the user id does not exist" should {
        "it should save the cash drawer" in new CashDrawersSyncFSpecContext {
          val cashDrawerId = UUID.randomUUID

          val upsertion = baseUpsertion.copy(locationId = rome.id)

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=$cashDrawerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerId ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion, withEmptyUserId = true)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "even if all the ids are random" should {
        "it should save the cash drawer" in new CashDrawersSyncFSpecContext {
          val cashDrawerId = UUID.randomUUID

          val upsertion = baseUpsertion

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=$cashDrawerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerId ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion, withEmptyLocationId = true, withEmptyUserId = true)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "even if a location has been soft-deleted" should {
        "it should save the cash drawer" in new CashDrawersSyncFSpecContext {
          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
          val anotherUser = Factory.user(merchant).create
          val cashDrawerId = UUID.randomUUID

          val upsertion = baseUpsertion.copy(locationId = deletedLocation.id, userId = anotherUser.id)

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=$cashDrawerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerId ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }

      "even if a user has been soft-deleted" should {
        "it should save the cash drawer" in new CashDrawersSyncFSpecContext {
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now)).create
          val cashDrawerId = UUID.randomUUID

          val upsertion = baseUpsertion.copy(locationId = rome.id, userId = deletedUser.id)

          Post(s"/v1/cash_drawers.sync?cash_drawer_id=$cashDrawerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val cashDrawerEntity = responseAs[ApiResponse[CashDrawerEntity]].data
            cashDrawerId ==== cashDrawerEntity.id
            assertUpsertion(cashDrawerEntity.id, upsertion)
            assertResponseById(cashDrawerEntity.id, cashDrawerEntity)
          }
        }
      }
    }
  }
}
