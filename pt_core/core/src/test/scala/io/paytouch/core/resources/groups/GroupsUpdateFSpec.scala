package io.paytouch.core.resources.groups

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GroupsUpdateFSpec extends GroupsFSpec {

  abstract class GroupUpdateResourceFSpecContext extends GroupResourceFSpecContext

  "POST /v1/groups.update?group_id=<group_id>" in {
    "if request has valid token" in {
      "if group doesn't exist yet" should {
        "return 404" in new GroupUpdateResourceFSpecContext {
          val newGroupId = UUID.randomUUID
          val groupUpdate = random[GroupUpdate]

          Post(s"/v1/groups.update?group_id=$newGroupId", groupUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if group already exists" should {
        "update group and return 200" in new GroupUpdateResourceFSpecContext {
          val globalCustomer = Factory.globalCustomer(merchant = Some(merchant)).create

          val group = Factory.group(merchant).create
          val groupUpdate = random[GroupUpdate].copy(customerIds = Some(Seq(globalCustomer.id)))

          Post(s"/v1/groups.update?group_id=${group.id}", groupUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(groupUpdate, group.id)
            assertResponseById(responseAs[ApiResponse[Group]].data, group.id)
          }
        }

        "update only the group name" in new GroupUpdateResourceFSpecContext {
          val globalCustomer = Factory.globalCustomer(merchant = Some(merchant)).create

          val groupName = "Awesomeness Group"

          val group = Factory.group(merchant).create
          Factory.customerGroup(globalCustomer, group).create

          val groupNameUpdate = GroupUpdate(name = Some(groupName), customerIds = None)

          Post(s"/v1/groups.update?group_id=${group.id}", groupNameUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val groupRecord = groupDao.findByIds(Seq(group.id)).await.head
            groupRecord.name ==== groupName

            val customerGroups = customerGroupDao.findByGroupId(groupRecord.id).await
            customerGroups.size ==== 1
            customerGroups.head.customerId ==== globalCustomer.id

            assertResponseById(responseAs[ApiResponse[Group]].data, group.id)
          }
        }

        "update only the group customer list" in new GroupUpdateResourceFSpecContext {
          val globalCustomerA = Factory.globalCustomer(merchant = Some(merchant)).create
          val globalCustomerB = Factory.globalCustomer(merchant = Some(merchant)).create
          val globalCustomerC = Factory.globalCustomer(merchant = Some(merchant)).create

          val group = Factory.group(merchant).create
          Factory.customerGroup(globalCustomerB, group).create
          Factory.customerGroup(globalCustomerC, group).create

          val groupCustomersUpdate = GroupUpdate(name = None, customerIds = Some(Seq(globalCustomerA.id)))

          Post(s"/v1/groups.update?group_id=${group.id}", groupCustomersUpdate)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val groupRecord = groupDao.findByIds(Seq(group.id)).await.head
            groupRecord.name ==== group.name

            val customerGroups = customerGroupDao.findByGroupId(groupRecord.id).await
            customerGroups.size ==== 1
            customerGroups.head.customerId ==== globalCustomerA.id

            assertResponseById(responseAs[ApiResponse[Group]].data, group.id)
          }
        }
      }
    }
  }
}
