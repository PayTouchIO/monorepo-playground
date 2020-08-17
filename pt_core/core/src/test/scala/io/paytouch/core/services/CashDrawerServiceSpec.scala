package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.model.enums.{ CashDrawerStatus, OrderStatus, PaymentStatus, Source }
import io.paytouch.core.data.model.CashDrawerRecord
import io.paytouch.core.expansions._
import io.paytouch.core.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities._
import io.paytouch.utils.Tagging._
import io.paytouch.core.utils.{ MockedRestApi, PaytouchLogger, ValidatedHelpers, FixtureDaoFactory => Factory }

class CashDrawerServiceSpec extends ServiceDaoSpec {

  abstract class CashDrawerServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    implicit val logger = new PaytouchLogger
    val service: CashDrawerService = wire[CashDrawerService]

    val cashDrawerId: UUID

    lazy val cashDrawerEntity = MockedRestApi
      .cashDrawerService
      .findById(cashDrawerId, MockedRestApi.cashDrawerService.defaultFilters)(NoExpansions())
      .await
      .get

    Factory.locationReceipt(rome).create
    val merchantEntity = MockedRestApi
      .merchantService
      .findById(merchant.id, MockedRestApi.merchantService.defaultFilters)(MerchantExpansions.none)
      .await
      .get
    val locationEntity = MockedRestApi
      .locationService
      .findById(rome.id, MockedRestApi.locationService.defaultFilters)(LocationExpansions.empty)
      .await
      .get
    val locationReceiptEntity = MockedRestApi.locationReceiptService.findByLocationId(rome.id).await.get
    val cashierEntity = MockedRestApi.userService.getUserInfoByIds(Seq(user.id)).await.head
    val userEntity = MockedRestApi
      .userService
      .findById(user.id, MockedRestApi.userService.defaultFilters)(UserExpansions.empty)
      .await
      .get
    val targetUsersEntities = Seq(userEntity)

    val baseUpsertion = random[CashDrawerUpsertion].copy(locationId = rome.id, appendActivities = None)

    lazy val prepareCashDrawerReportPayload =
      PrepareCashDrawerReportPayload(
        cashDrawerEntity,
        merchantEntity,
        targetUsersEntities,
        locationEntity,
        locationReceiptEntity,
        cashierEntity,
      )
    lazy val prepareCashDrawerReportMsg = PrepareCashDrawerReport(prepareCashDrawerReportPayload)
  }

  "CashDrawerService" in {
    "syncById" should {
      "if CashDrawerStatus = Ended" in {
        "send EntitySynced[CashDrawer] and PrepareCashDrawerReport messages" in new CashDrawerServiceSpecContext {
          val upsertion = baseUpsertion.copy(status = CashDrawerStatus.Ended)
          val cashDrawerId = UUID.randomUUID
          val (_, entity: CashDrawer) = service.syncById(cashDrawerId, upsertion)(userCtx).await

          val msg = EntitySynced[IdOnlyEntity](IdOnlyEntity(entity.id, entity.classShortName), rome.id)(userCtx)
          actorMock.expectMsg(SendMsgWithRetry(msg))
          actorMock.expectNoMessage()
        }
      }
      "if CashDrawerStatus not Ended" in {
        "send EntitySynced[CashDrawer] messages" in new CashDrawerServiceSpecContext {
          val upsertion = baseUpsertion.copy(status = CashDrawerStatus.Started)
          val cashDrawerId = UUID.randomUUID
          val (_, entity: CashDrawer) = service.syncById(cashDrawerId, upsertion)(userCtx).await

          val msg = EntitySynced[IdOnlyEntity](IdOnlyEntity(entity.id, entity.classShortName), rome.id)(userCtx)
          actorMock.expectMsg(SendMsgWithRetry(msg))
          actorMock.expectNoMessage()
        }
      }
    }

    "sendReport" should {

      "send PrepareCashDrawerReport message" in new CashDrawerServiceSpecContext {
        val cashDrawer = Factory.cashDrawer(user, rome).create
        val cashDrawerId = cashDrawer.id

        service.sendReport(cashDrawer.id)(userCtx).await

        actorMock.expectMsg(SendMsgWithRetry(prepareCashDrawerReportMsg))
        actorMock.expectNoMessage()
      }
    }
  }
}
