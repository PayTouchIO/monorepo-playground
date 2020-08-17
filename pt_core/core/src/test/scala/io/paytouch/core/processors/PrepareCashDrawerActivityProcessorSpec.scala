package io.paytouch.core.processors

import scala.concurrent._

import awscala.s3.Bucket

import com.softwaremill.macwire.wire

import io.paytouch.core.{ S3CashDrawerActivitiesBucket, S3ImagesBucket }
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities.{
  CashDrawerReport,
  CashDrawerReportPayload,
  PrepareCashDrawerReport,
  PrepareCashDrawerReportPayload,
}
import io.paytouch.core.data.model.{ CashDrawerRecord, Permission, CashDrawerUpdate => CashDrawerUpdateModel }
import io.paytouch.core.utils.{ MockedRestApi, MultipleLocationFixtures, FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class PrepareCashDrawerActivityProcessorSpec extends ProcessorSpec {
  abstract class PrepareCashDrawerActivityProcessorSpecContext
      extends ProcessorSpecContext
         with MultipleLocationFixtures {
    implicit val u: UserContext = userContext

    val cashDrawer = Factory.cashDrawer(user, rome).create
    Factory.cashDrawerActivity(cashDrawer).create
    Factory.cashDrawerActivity(cashDrawer).create

    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val cashDrawerService = MockedRestApi.cashDrawerService
    val cashDrawerActivityService = MockedRestApi.cashDrawerActivityService
    val userService = MockedRestApi.userService
    val s3ClientMock = mock[S3Client]
    val imageMockBucket = mock[Bucket].taggedWith[S3CashDrawerActivitiesBucket]

    val processor = wire[PrepareCashDrawerActivityProcessor]

    @scala.annotation.nowarn("msg=Auto-application")
    val payload = {
      val value =
        random[PrepareCashDrawerReportPayload]

      value.copy(
        targetUsers = Seq(random[User].copy(email = user.email)),
        cashDrawer = value.cashDrawer.copy(id = cashDrawer.id),
        cashier = value.cashier.copy(id = user.id),
      )
    }

    val msg = PrepareCashDrawerReport(payload)
    val s3FileUrl = "aws-s3-file-url"

    val expectedMsg = {
      val prepareData = msg.payload.data
      val payload = CashDrawerReportPayload(s3FileUrl, prepareData)
      SendMsgWithRetry(CashDrawerReport(user.email, payload))
    }

    def assertCashDrawerHasExportFilename() =
      daos.cashDrawerDao.findById(cashDrawer.id).await.get.exportFilename ==== Some(s3FileUrl)

    def assertCashDrawerReportSent() = actorMock.expectMsg(expectedMsg)

  }

  "PrepareCashDrawerActivityProcessor" in {

    "run" in new PrepareCashDrawerActivityProcessorSpecContext {
      s3ClientMock.uploadPublicFileToBucket(any, any)(any) returns Future.successful(s3FileUrl)

      processor.execute(msg)

      afterAWhile {
        assertCashDrawerHasExportFilename()
        assertCashDrawerReportSent()
        ok
      }
    }
  }
}
