package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._

import io.paytouch.core.async.monitors.AuthenticationMonitor
import io.paytouch.core.async.sqs._
import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.data.model.enums.{ BusinessType, ImageUploadType, MerchantMode, SetupType }
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ MerchantSetupStatus, MerchantSetupSteps }
import io.paytouch.core.entities.enums.MerchantSetupSteps._
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities.MerchantChanged
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory, PaytouchLogger }
import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.utils.Tagging._

class AdminMerchantServiceSpec extends ServiceDaoSpec {
  abstract class AdminMerchantServiceSpecContext extends ServiceDaoSpecContext {
    implicit val logger = new PaytouchLogger

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val bcryptRounds = Config.bcryptRounds

    // We don't care about sample data being created for demo merchants
    override val sampleDataService = mock[SampleDataService]

    val authenticationMonitor = actorMock.ref.taggedWith[AuthenticationMonitor]
    val authenticationService: AuthenticationService = wire[AuthenticationService]

    lazy val service: AdminMerchantService = wire[AdminMerchantService]
    override val merchantService = wire[MerchantService]

    val merchantDao = daos.merchantDao
  }

  "AdminMerchantService" in {
    "create" should {
      "mode = demo" should {
        "not send a message to ordering" in new AdminMerchantServiceSpecContext {
          val merchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation = random[PublicMerchantCreation].copy(
            mode = MerchantMode.Demo,
            setupType = SetupType.Paytouch,
            email = genEmail.instance,
          )

          val (resultType, entity) = service.create(merchantId, creation.toMerchantCreation).await.success
          entity.id ==== merchantId

          actorMock.expectNoMessage()
        }
      }

      "mode = production" should {
        "send a message to ordering" in new AdminMerchantServiceSpecContext {
          val merchantId = UUID.randomUUID

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[PublicMerchantCreation].copy(
              mode = MerchantMode.Production,
              setupType = SetupType.Paytouch,
              email = genEmail.instance,
            )

          val (resultType, entity) = service.create(merchantId, creation.toMerchantCreation).await.success
          entity.id ==== merchantId

          val merchantRecord = merchantDao.findById(merchantId).await.get
          val merchantEntity =
            merchantService
              .findById(merchantId)(merchantService.defaultExpansions)
              .await
              .get

          actorMock.expectMsg(SendMsgWithRetry(MerchantChanged(merchantEntity, merchantRecord.paymentProcessorConfig)))
        }
      }
    }
  }
}
