package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object TaxRateSeeds extends Seeds {

  lazy val taxRateDao = daos.taxRateDao

  def load(implicit user: UserRecord): Future[Seq[TaxRateRecord]] = {
    val taxRateIds = taxRateIdsPerEmail(user.email)

    val taxRates = taxRateIds.map { taxRateId =>
      TaxRateUpdate(
        id = Some(taxRateId),
        merchantId = Some(user.merchantId),
        name = Some(randomWords),
        value = Some(genBigDecimal.instance),
        applyToPrice = None,
      )
    }

    taxRateDao.bulkUpsert(taxRates).extractRecords
  }
}
