package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.entities.ResettableBigDecimal
import io.paytouch.seeds.IdsProvider._
import org.scalacheck.Gen

import scala.concurrent._

object CashDrawerSeeds extends Seeds {

  lazy val cashDrawerDao = daos.cashDrawerDao

  def load(userLocations: Seq[UserLocationRecord])(implicit user: UserRecord): Future[Seq[CashDrawerRecord]] = {
    val cashDrawerIds = cashDrawerIdsPerEmail(user.email)

    val cashDrawers = cashDrawerIds.map { cashDrawerId =>
      val userLocation = userLocations.random
      CashDrawerUpdate(
        id = Some(cashDrawerId),
        merchantId = Some(user.merchantId),
        locationId = Some(userLocation.locationId),
        userId = Some(userLocation.userId),
        employeeId = None,
        name = "Cash Drawer",
        startingCashAmount = Some(genBigDecimal.instance),
        endingCashAmount = genBigDecimal.instance,
        cashSalesAmount = genBigDecimal.instance,
        cashRefundsAmount = genBigDecimal.instance,
        paidInAndOutAmount = genBigDecimal.instance,
        paidInAmount = genBigDecimal.instance,
        paidOutAmount = genBigDecimal.instance,
        manualPaidInAmount = genBigDecimal.instance,
        manualPaidOutAmount = genBigDecimal.instance,
        tippedInAmount = genBigDecimal.instance,
        tippedOutAmount = genBigDecimal.instance,
        expectedAmount = genBigDecimal.instance,
        status = Some(genCashDrawerStatus.instance),
        startedAt = Some(genZonedDateTime.instance),
        endedAt = Gen.option(genZonedDateTime).instance,
        exportFilename = None,
        printerMacAddress = None,
      )
    }

    cashDrawerDao.bulkUpsert(cashDrawers).extractRecords
  }
}
