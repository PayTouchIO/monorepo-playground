package io.paytouch.seeds

import scala.concurrent._

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model._

object CashDrawerActivitySeeds extends Seeds {
  lazy val cashDrawerActivityDao = daos.cashDrawerActivityDao

  def load(cashDrawers: Seq[CashDrawerRecord])(implicit user: UserRecord): Future[Seq[CashDrawerActivityRecord]] = {

    val cashDrawerActivity = cashDrawers.flatMap { cashDrawer =>
      (0 to 50).randomSample.map { _ =>
        CashDrawerActivityUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          userId = cashDrawer.userId,
          orderId = UUID.randomUUID.some,
          cashDrawerId = Some(cashDrawer.id),
          `type` = Some(genCashDrawerActivityType.instance),
          startingCashAmount = Some(genBigDecimal.instance),
          endingCashAmount = Some(genBigDecimal.instance),
          payInAmount = Some(genBigDecimal.instance),
          payOutAmount = Some(genBigDecimal.instance),
          tipInAmount = Some(genBigDecimal.instance),
          tipOutAmount = Some(genBigDecimal.instance),
          currentBalanceAmount = Some(genBigDecimal.instance),
          timestamp = Some(genZonedDateTime.instance),
          notes = genResettableString.instance,
          tipForUserId = None,
        )
      }
    }

    cashDrawerActivityDao.bulkUpsert(cashDrawerActivity).extractRecords
  }
}
