package io.paytouch.core.resources.cashdrawers

import java.util.UUID

import io.paytouch.core.data.model.CashDrawerRecord
import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, _ }
import io.paytouch.core.utils._

abstract class CashDrawersFSpec extends FSpec {

  abstract class CashDrawerResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val cashDrawerDao = daos.cashDrawerDao
    val cashDrawerActivityDao = daos.cashDrawerActivityDao

    def assertResponseById(recordId: UUID, entity: CashDrawerEntity) = {
      val record = cashDrawerDao.findById(recordId).await.get
      assertResponse(record, entity)
    }

    def assertResponse(record: CashDrawerRecord, entity: CashDrawerEntity) = {
      entity.id ==== record.id
      entity.locationId ==== record.locationId
      entity.userId ==== record.userId
      entity.startingCash ==== MonetaryAmount(record.startingCashAmount, currency)
      entity.endingCash ==== record.endingCashAmount.map(am => MonetaryAmount(am, currency))
      entity.cashSales ==== record.cashSalesAmount.map(am => MonetaryAmount(am, currency))
      entity.cashRefunds ==== record.cashRefundsAmount.map(am => MonetaryAmount(am, currency))
      entity.paidInAndOut ==== record.paidInAndOutAmount.map(am => MonetaryAmount(am, currency))
      entity.expected ==== record.expectedAmount.map(am => MonetaryAmount(am, currency))
      entity.status ==== record.status
      entity.startedAt ==== record.startedAt
      entity.endedAt ==== record.endedAt
    }
  }
}
