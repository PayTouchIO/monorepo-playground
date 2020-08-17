package io.paytouch.core.data.daos

import io.paytouch.core.utils.{ MultipleLocationFixtures, FixtureDaoFactory => Factory }

class TaxRateDaoSpec extends DaoSpec {

  lazy val taxRateDao = daos.taxRateDao

  abstract class TaxRateDaoSpecContext extends DaoSpecContext with MultipleLocationFixtures

  "TaxRateDao" in {
    "delete" should {
      "work even if related to an order tax rate" in new TaxRateDaoSpecContext {
        val taxRate = Factory.taxRate(merchant).create
        val order = Factory.order(merchant).create
        Factory.orderTaxRate(order, taxRate).create
        taxRateDao.deleteById(taxRate.id).await
      }
    }
  }
}
