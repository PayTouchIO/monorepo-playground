package io.paytouch.core.resources.passes

import java.util.UUID

import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import io.paytouch.core.entities.enums.{ PassItemType, PassType }
import io.paytouch.core.utils.{ MockedRestApi, UtcTime, FixtureDaoFactory => Factory }

class PassesInstallLoyaltyMembershipFSpec extends PassesFSpec {

  abstract class PassesInstallLoyaltyMembershipFSpecContext extends PassResourceFSpecContext {
    val passIosPublicUrl = "foo"
    val androidPassPublicUrl = "bar"
    val loyaltyProgram = Factory.loyaltyProgram(merchant).create
    val globalCustomer = Factory.globalCustomer(Some(merchant)).create
    val passService = MockedRestApi.passService

    def assertClickTimeIsStored(id: UUID) = reloadLoyaltyMemberships(id).customerOptInAt must beSome

    def reloadLoyaltyMemberships(id: UUID) = daos.loyaltyMembershipDao.findById(id).await.get
  }

  "GET /v1/public/passes.install?id=$&type=[android|ios]" in {
    "if request has valid token" in {
      "if the id doesn't exist in customer loyalty program table" should {
        "return 404" in new PassesInstallLoyaltyMembershipFSpecContext {
          val randomUuid = UUID.randomUUID
          val path = passService.generatePath(randomUuid, PassType.Ios, PassItemType.LoyaltyMembership)

          Get(path) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
      "if the id exists but pass url is not set customer loyalty program table" should {
        "return 404" in new PassesInstallLoyaltyMembershipFSpecContext {
          val loyaltyMembership =
            Factory.loyaltyMembership(globalCustomer, loyaltyProgram, iosPassPublicUrl = None).create
          val path = passService.generatePath(loyaltyMembership.id, PassType.Ios, PassItemType.LoyaltyMembership)

          Get(path) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
      "if the id exists in customer loyalty program table" in {
        "if type=ios" should {
          "redirect" in new PassesInstallLoyaltyMembershipFSpecContext {
            val loyaltyMembership = Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram, iosPassPublicUrl = Some(passIosPublicUrl))
              .create
            val path = passService.generatePath(loyaltyMembership.id, PassType.Ios, PassItemType.LoyaltyMembership)

            Get(path) ~> routes ~> check {
              assertStatus(StatusCodes.Found)
              header[Location] ==== Some(Location(passIosPublicUrl))
              assertClickTimeIsStored(loyaltyMembership.id)
            }
          }
        }
        "if type=android" should {
          "redirect" in new PassesInstallLoyaltyMembershipFSpecContext {
            val loyaltyMembership = Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram, androidPassPublicUrl = Some(androidPassPublicUrl))
              .create
            val path =
              passService.generatePath(loyaltyMembership.id, PassType.Android, PassItemType.LoyaltyMembership)

            Get(path) ~> routes ~> check {
              assertStatus(StatusCodes.Found)
              header[Location] ==== Some(Location(androidPassPublicUrl))
              assertClickTimeIsStored(loyaltyMembership.id)
            }
          }
        }
        "if url is None" should {
          "return not found" in new PassesInstallLoyaltyMembershipFSpecContext {
            val loyaltyMembership = Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram, androidPassPublicUrl = None, iosPassPublicUrl = None)
              .create
            val path =
              passService.generatePath(loyaltyMembership.id, PassType.Android, PassItemType.LoyaltyMembership)

            Get(path) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
        "if passOptInColumn has already a value" should {
          "redirect without updating it" in new PassesInstallLoyaltyMembershipFSpecContext {
            val loyaltyMembership = Factory
              .loyaltyMembership(globalCustomer, loyaltyProgram, customerOptInAt = Some(UtcTime.now))
              .create
            val path =
              passService.generatePath(loyaltyMembership.id, PassType.Android, PassItemType.LoyaltyMembership)

            Get(path) ~> routes ~> check {
              reloadLoyaltyMemberships(loyaltyMembership.id).customerOptInAt ==== loyaltyMembership.customerOptInAt
            }
          }
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new PassesInstallLoyaltyMembershipFSpecContext {
        val loyaltyMembership = Factory
          .loyaltyMembership(globalCustomer, loyaltyProgram, iosPassPublicUrl = Some(passIosPublicUrl))
          .create
        Get(s"/v1/public/passes.install?id=${loyaltyMembership.id}&type=ios&token=randomtoken") ~> routes ~> check {
          rejection should beAnInstanceOf[AuthorizationFailedRejection]
        }
      }
    }
  }
}
