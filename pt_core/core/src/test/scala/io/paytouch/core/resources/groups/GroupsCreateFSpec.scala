package io.paytouch.core.resources.groups

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GroupsCreateFSpec extends GroupsFSpec {

  abstract class GroupCreateResourceFSpecContext extends GroupResourceFSpecContext

  "POST /v1/groups.create?group_id=$" in {
    "if request has valid token" in {
      "create group and return 201" in new GroupCreateResourceFSpecContext {
        val globalCustomer = Factory.globalCustomer().create
        val customer = Factory.customerMerchant(merchant, globalCustomer).create

        val newGroupId = UUID.randomUUID
        val groupCreation = random[GroupCreation].copy(customerIds = Seq(customer.id))

        Post(s"/v1/groups.create?group_id=$newGroupId", groupCreation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val groupResponse = responseAs[ApiResponse[Group]].data
          assertUpdate(groupCreation.asUpdate, groupResponse.id)
          assertResponseById(groupResponse, groupResponse.id)
        }
      }
    }
  }
}
