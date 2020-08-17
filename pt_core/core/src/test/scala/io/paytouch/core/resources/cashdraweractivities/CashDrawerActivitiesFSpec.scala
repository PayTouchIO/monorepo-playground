package io.paytouch.core.resources.cashdraweractivities

import java.util.UUID

import io.paytouch.core.data.model.CashDrawerActivityRecord
import io.paytouch.core.entities.{ CashDrawerActivity => CashDrawerActivityEntity, _ }
import io.paytouch.core.utils._

abstract class CashDrawerActivitiesFSpec extends FSpec {

  abstract class CashDrawerActivityResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val cashDrawerActivityDao = daos.cashDrawerActivityDao

    def assertResponseById(recordId: UUID, entity: CashDrawerActivityEntity) = {
      val record = cashDrawerActivityDao.findById(recordId).await.get
      assertResponse(record, entity)
    }

    def assertResponse(record: CashDrawerActivityRecord, entity: CashDrawerActivityEntity) = {
      entity.id ==== record.id
      entity.cashDrawerId ==== record.cashDrawerId
      entity.userId ==== record.userId
      entity.`type` ==== record.`type`
      entity.startingCash ==== record.startingCashAmount.map(am => MonetaryAmount(am, currency))
      entity.endingCash ==== record.endingCashAmount.map(am => MonetaryAmount(am, currency))
      entity.payIn ==== record.payInAmount.map(am => MonetaryAmount(am, currency))
      entity.payOut ==== record.payOutAmount.map(am => MonetaryAmount(am, currency))
      entity.tipIn ==== record.tipInAmount.map(am => MonetaryAmount(am, currency))
      entity.tipOut ==== record.tipOutAmount.map(am => MonetaryAmount(am, currency))
      entity.currentBalance ==== MonetaryAmount(record.currentBalanceAmount, currency)
      entity.timestamp ==== record.timestamp
      entity.notes ==== record.notes
    }
  }
}
