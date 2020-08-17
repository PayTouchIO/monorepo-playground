package io.paytouch.core.resources.groups

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GroupsDeleteFSpec extends GroupsFSpec {

  abstract class GroupDeleteResourceFSpecContext extends GroupResourceFSpecContext {
    def assertGroupDoesntExist(id: UUID) = groupDao.findById(id).await should beNone
    def assertGroupExists(id: UUID) = groupDao.findById(id).await should beSome
  }

  "POST /v1/groups.delete" in {

    "if request has valid token" in {
      "if group doesn't exist" should {
        "do nothing and return 204" in new GroupDeleteResourceFSpecContext {
          val nonExistingGroupId = UUID.randomUUID

          Post(s"/v1/groups.delete", Ids(ids = Seq(nonExistingGroupId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertGroupDoesntExist(nonExistingGroupId)
          }
        }
      }

      "if group belongs to the merchant" should {
        "delete the group and return 204" in new GroupDeleteResourceFSpecContext {
          val globalCustomer1 = Factory.globalCustomer(Some(merchant)).create
          val globalCustomer2 = Factory.globalCustomer(Some(merchant)).create
          val globalCustomer3 = Factory.globalCustomer(Some(merchant)).create

          val group = Factory.group(merchant).create
          Factory.customerGroup(globalCustomer1, group).create
          Factory.customerGroup(globalCustomer2, group).create
          Factory.customerGroup(globalCustomer3, group).create

          Post(s"/v1/groups.delete", Ids(ids = Seq(group.id))).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertGroupDoesntExist(group.id)
            customerGroupDao.findByGroupId(group.id).await ==== Seq.empty
          }
        }
      }

      "if group belongs to a different merchant" should {
        "do not delete the group and return 204" in new GroupDeleteResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorGroup = Factory.group(competitor).create

          Post(s"/v1/groups.delete", Ids(ids = Seq(competitorGroup.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertGroupExists(competitorGroup.id)
          }
        }
      }
    }
  }
}
