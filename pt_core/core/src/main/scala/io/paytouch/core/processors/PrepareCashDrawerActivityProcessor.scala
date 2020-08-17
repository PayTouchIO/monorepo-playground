package io.paytouch.core.processors

import java.io.File
import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import awscala.s3.Bucket

import cats.data._
import cats.implicits._

import com.github.tototoshi.csv.CSVWriter

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.{ withTag, S3CashDrawerActivitiesBucket }
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.CashDrawerActivityType._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ContextSource
import io.paytouch.core.filters.CashDrawerActivityFilters
import io.paytouch.core.messages.entities.{ PrepareCashDrawerReport, SQSMessage }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.{ CashDrawerActivityService, CashDrawerService, LocationService, UserService }
import io.paytouch.core.utils.UtcTime

class PrepareCashDrawerActivityProcessor(
    val cashDrawerService: CashDrawerService,
    val cashDrawerActivityService: CashDrawerActivityService,
    val userService: UserService,
    val s3Client: S3Client,
    val uploadBucket: Bucket withTag S3CashDrawerActivitiesBucket,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor
       with LazyLogging {

  implicit private lazy val bucket = uploadBucket

  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: PrepareCashDrawerReport => processPrepareCashDrawerReport(msg)
  }

  private def processPrepareCashDrawerReport(msg: PrepareCashDrawerReport): Future[Unit] = {
    val merchantId = msg.payload.merchantId
    val payloadData = msg.payload.data
    val cashDrawer = payloadData.cashDrawer
    val location = payloadData.location
    val userId = payloadData.cashier.id
    val pagination = Pagination(1, 100)

    (for {
      userContext <- OptionT(userService.getUserContext(userId, ContextSource.PtDashboard))
      (activities, _) <- OptionT.liftF(
        cashDrawerActivityService.findAll(CashDrawerActivityFilters(cashDrawer.id))(
          cashDrawerActivityService.defaultExpansions.copy(withUserInfo = true),
        )(userContext, pagination),
      )
      convertedActivities <- OptionT.pure[Future](convertData(location, activities))
      localFile <- OptionT.pure[Future](writeToFile(convertedActivities))
      filename <- OptionT.liftF(uploadFileTos3(cashDrawer, merchantId, localFile))
      _ <- OptionT.liftF(cashDrawerService.storeExportS3Filename(cashDrawer.id, filename))
      _ <- OptionT.pure[Future](sendMessage(msg, filename))
    } yield ()).value.void
  }

  private def writeToFile(data: Seq[Seq[String]]): File = {
    val file = File.createTempFile(s"activities-${UtcTime.now}-${UUID.randomUUID}", ".csv")
    val writer = CSVWriter.open(file)
    writer.writeAll(data)
    file
  }

  private def formatDateAtTz(datetime: ZonedDateTime)(implicit location: Location): String =
    datetime.toLocationTimezone(location.timezone).toLocalDate.toString
  private def formatTimeAtTz(datetime: ZonedDateTime)(implicit location: Location): String =
    datetime.toLocationTimezone(location.timezone).toLocalTime.toString

  private def convertData(location: Location, data: Seq[CashDrawerActivity]): Seq[Seq[String]] = {
    implicit val l = location
    val headers = Seq(
      Seq(
        "Date",
        "Time",
        "Employee Name",
        "Type",
        "Pay In",
        "Pay Out",
        "Balance",
        "Notes",
      ),
    )

    val rows = data.map { row =>
      Seq(
        formatDateAtTz(row.timestamp),
        formatTimeAtTz(row.timestamp),
        row.user.map(_.fullName).getOrElse(""),
        row.`type` match {
          case Create         => "Create"
          case Sale           => "Cash Sale"
          case Refund         => "Cash Refund"
          case PayIn          => "Pay In"
          case PayOut         => "Pay Out"
          case TipIn          => "Tip In"
          case TipOut         => "Tip Out"
          case NoSale         => "No Sale"
          case ValuesOverride => "Values Override"
          case StartCash      => "Start Cash"
          case EndCash        => "End Cash"
        },
        row.payIn.map(amount => amount.show).getOrElse(""),
        row.payOut.map(amount => amount.show).getOrElse(""),
        row.currentBalance.show,
        row.notes.getOrElse(""),
      )
    }
    headers ++ rows
  }

  private def uploadFileTos3(
      cashDrawer: CashDrawer,
      merchantId: UUID,
      localFile: File,
    ): Future[String] = {
    val keys = Seq(merchantId, s"${cashDrawer.id}.csv")
    logger.info(s"[Cash Drawer Activity File Upload ${cashDrawer.id}] uploading csv file to S3")
    s3Client.uploadPublicFileToBucket(keys.mkString("/"), localFile)
  }

  private def sendMessage(msg: PrepareCashDrawerReport, activitiesFileUrl: String): Unit =
    messageHandler.sendCashDrawerReport(msg, activitiesFileUrl)
}
