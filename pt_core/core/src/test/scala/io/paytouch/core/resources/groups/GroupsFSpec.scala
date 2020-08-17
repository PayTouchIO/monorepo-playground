package io.paytouch.core.resources.groups

import java.util.UUID

import io.paytouch.core.data.model.GroupRecord
import io.paytouch.core.entities._
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.utils._

abstract class GroupsFSpec extends FSpec {
  abstract class GroupResourceFSpecContext extends FSpecContext with JsonSupport with MultipleLocationFixtures {
    val groupDao = daos.groupDao
    val customerGroupDao = daos.customerGroupDao

    def assertUpdate(groupUpdate: GroupUpdate, newGroupId: UUID) = {
      val dbGroup = groupDao.findById(newGroupId).await.get
      val dbCustomerIds = customerGroupDao.findByGroupIdAndMerchantId(newGroupId, merchant.id).await.map(_.customerId)
      if (groupUpdate.name.isDefined) groupUpdate.name ==== Some(dbGroup.name)
      if (groupUpdate.customerIds.isDefined) dbCustomerIds must containTheSameElementsAs(groupUpdate.customerIds.get)
    }

    def assertResponseById(groupEntity: Group, groupId: UUID) = {
      val groupRecord = groupDao.findById(groupId).await.get
      assertResponse(groupEntity, groupRecord)
    }

    def assertResponse(
        groupEntity: Group,
        groupRecord: GroupRecord,
        customerIds: Seq[UUID] = Seq.empty,
        customersCount: Option[Int] = None,
        revenues: Seq[MonetaryAmount] = Seq.empty,
        visits: Option[Int] = None,
      ) = {
      groupEntity.name ==== groupRecord.name

      groupEntity.customers.getOrElse(Seq.empty).map(_.id) must containTheSameElementsAs(customerIds)
      if (customersCount.isDefined) groupEntity.customersCount ==== customersCount
      groupEntity.revenues.getOrElse(Seq.empty) must containTheSameElementsAs(revenues)
      if (visits.isDefined) groupEntity.visits ==== visits
    }
  }
}
