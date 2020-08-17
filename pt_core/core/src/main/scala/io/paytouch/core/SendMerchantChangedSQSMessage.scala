package io.paytouch.core

import cats.implicits._

import slick.lifted.TableQuery

import io.paytouch._
import io.paytouch.implicits._
import io.paytouch.core.data.tables._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.PaymentProcessorConfig

import scala.concurrent._
import scala.concurrent.duration._

/**
  * Sends MerchantChanged SQS message to ordering
  */
object SendMerchantChangedSQSMessage extends PtCore with MigrationHelpers {
  val dieAfter = 10.minutes

  run()

  def run(): Unit = {
    val table = TableQuery[MerchantsTable]
    val queries = pageQueries(db, table, pageSize = 1000)
    val total = queries.size
    val zipped = queries.zipWithIndex.map(_.map(_ + 1))
    val merchantDao = daos.merchantDao

    zipped.foreach {
      case (page, index) =>
        println(
          s"Running page ${index.cyan}/${total.cyan} for ${"MerchantsTable".cyan} with ${size(db, table).cyan} rows...",
        )

        page
          .result
          .pipe(db.run)
          .pipe(await)
          .foreach { merchant =>
            merchant.paymentProcessorConfig match {
              case _: PaymentProcessorConfig.Stripe | _: PaymentProcessorConfig.Worldpay |
                  _: PaymentProcessorConfig.Paytouch =>
                merchantService.sendMerchantChangedMessage(merchant)
              case _ =>
                // Ignore other payment processor configs, as the merchant
                // changed message doesn't support them, and we don't want to
                // override existing stores.
                Future.unit
            }
          }
    }

    println(s"Messages dispatched to actor - will exit ${"hard".red} after ${dieAfter.cyan}")

    // Unfortunately the "proper way" is INSANELY complicated
    Thread.sleep(dieAfter.toMillis)

    system.terminate()
  }
}
