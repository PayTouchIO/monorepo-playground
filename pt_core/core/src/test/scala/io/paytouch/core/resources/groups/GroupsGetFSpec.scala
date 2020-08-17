package io.paytouch.core.resources.groups

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ Group => GroupEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GroupsGetFSpec extends GroupsFSpec {

  abstract class GroupGetResourceFSpecContext extends GroupResourceFSpecContext

  "GET /v1/group.get?group_id=<group_id>" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return group" in new GroupGetResourceFSpecContext {
          val group = Factory.group(merchant).create

          Get(s"/v1/groups.get?group_id=${group.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groupResponse = responseAs[ApiResponse[GroupEntity]].data
            groupResponse.id ==== group.id
            assertResponse(groupResponse, group)
          }
        }
      }
      "with expand[]=customers parameter" should {
        "return group with expanded customers" in new GroupGetResourceFSpecContext {
          val group = Factory.group(merchant).create
          val globalCustomer1 = Factory.globalCustomer().create
          val customer1 = Factory.customerMerchant(merchant, globalCustomer1).create
          val globalCustomer2 = Factory.globalCustomer().create
          val customer2 = Factory.customerMerchant(merchant, globalCustomer2).create

          Factory.customerLocation(globalCustomer1, rome).create
          Factory.customerLocation(globalCustomer2, rome).create

          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create

          Get(s"/v1/groups.get?group_id=${group.id}&expand[]=customers")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val groupResponse = responseAs[ApiResponse[GroupEntity]].data
            groupResponse.id ==== group.id
            assertResponse(groupResponse, group, customerIds = Seq(customer1.id, customer2.id))
          }
        }
      }
    }
  }
}
