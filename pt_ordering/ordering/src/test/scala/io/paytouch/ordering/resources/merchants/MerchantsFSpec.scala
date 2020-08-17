package io.paytouch.ordering.resources.merchants

import java.util.UUID

import io.paytouch.ordering.data.model.MerchantRecord
import io.paytouch.ordering.entities._
import io.paytouch.ordering.utils.{ FSpec, MultipleLocationFixtures }

abstract class MerchantsFSpec extends FSpec {

  abstract class MerchantResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val storeDao = daos.storeDao
    val merchantDao = daos.merchantDao

    def assertResponseById(id: UUID, entity: Merchant) = {
      val record = merchantDao.findById(id).await.get
      assertResponse(entity, record)
    }

    def assertResponse(entity: Merchant, record: MerchantRecord) = {
      entity.id ==== record.id
      entity.urlSlug ==== record.urlSlug
    }

    def assertUpdate(id: UUID, update: MerchantUpdate) = {
      val record = merchantDao.findById(id).await.get

      id ==== record.id
      if (update.urlSlug.isDefined) update.urlSlug ==== Some(record.urlSlug)
    }
  }
}
