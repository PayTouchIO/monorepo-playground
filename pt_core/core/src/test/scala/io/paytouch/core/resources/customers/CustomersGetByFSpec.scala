package io.paytouch.core.resources.customers

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import MonetaryAmount._
import io.paytouch.core.data.model.LoyaltyMembershipRecord
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CustomersGetByFSpec extends CustomersFSpec {

  class CustomersGetByFSpecContext extends CustomerResourceFSpecContext {
    val customer = Factory.globalCustomer(Some(merchant)).create
    val loyaltyProgram = Factory.loyaltyProgram(merchant, pointsToReward = Some(100)).create

    def baseLoyaltyMembership(loyaltyMembership: LoyaltyMembershipRecord) =
      LoyaltyMembership(
        id = loyaltyMembership.id,
        customerId = loyaltyMembership.customerId,
        loyaltyProgramId = loyaltyMembership.loyaltyProgramId,
        lookupId = loyaltyMembership.lookupId,
        points = loyaltyMembership.points,
        pointsToNextReward = 100 - loyaltyMembership.points,
        passPublicUrls = PassUrls(),
        customerOptInAt = loyaltyMembership.customerOptInAt,
        merchantOptInAt = loyaltyMembership.merchantOptInAt,
        enrolled = false,
        visits = 0,
        totalSpend = 0.$$$,
      )
  }

  "GET /v1/customers.get_by?loyalty_lookup_id=<>" in {
    "if request has valid token" in {

      "with no params" should {
        "return data with loyalty statuses for currently joined loyalty programs filtered by loyalty program id" in new CustomersGetByFSpecContext {
          val loyaltyMembership =
            Factory.loyaltyMembership(customer, loyaltyProgram).create

          val loyaltyProgramB = Factory.loyaltyProgram(merchant).create
          val loyaltyMembershipB =
            Factory
              .loyaltyMembership(customer, loyaltyProgramB)
              .create

          Factory.customerLocation(customer, rome, totalVisits = Some(5), totalSpend = Some(10)).create
          Factory.customerLocation(customer, london, totalVisits = Some(15), totalSpend = Some(20)).create

          Get(s"/v1/customers.get_by?loyalty_lookup_id=${loyaltyMembership.lookupId}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== customer.id
            val expectedLoyaltyMembership = baseLoyaltyMembership(loyaltyMembership).copy(
              visits = 20,
              totalSpend = 30.$$$,
            )
            assertCustomerResponse(
              customerResponse,
              loyaltyProgramIds = Some(Seq(loyaltyProgramB.id, loyaltyProgram.id)),
              loyaltyMemberships = Some(Seq(expectedLoyaltyMembership)),
              totalVisits = Some(20),
              totalSpend = Some(Seq(30.$$$)),
              avgTips = Some(Seq.empty),
            )
          }
        }

        "if customerOptInAt is set" should {
          "have loyalty status enrolled=true" in new CustomersGetByFSpecContext {
            val loyaltyMembership =
              Factory.loyaltyMembership(customer, loyaltyProgram, customerOptInAt = Some(UtcTime.now)).create

            Get(s"/v1/customers.get_by?loyalty_lookup_id=${loyaltyMembership.lookupId}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              val expectedLoyaltyMembership = baseLoyaltyMembership(loyaltyMembership).copy(enrolled = true)
              assertFullyExpandedCustomerResponse(
                customerResponse,
                loyaltyProgramIds = Seq(loyaltyProgram.id),
                loyaltyMemberships = Some(Seq(expectedLoyaltyMembership)),
              )
            }
          }
        }
        "if merchantOptInAt is set" should {
          "have loyalty status enrolled=true" in new CustomersGetByFSpecContext {
            val loyaltyMembership =
              Factory.loyaltyMembership(customer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            Get(s"/v1/customers.get_by?loyalty_lookup_id=${loyaltyMembership.lookupId}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              val expectedLoyaltyMembership = baseLoyaltyMembership(loyaltyMembership).copy(enrolled = true)
              assertFullyExpandedCustomerResponse(
                customerResponse,
                loyaltyProgramIds = Seq(loyaltyProgram.id),
                loyaltyMemberships = Some(Seq(expectedLoyaltyMembership)),
              )
            }
          }
        }

      }
    }
  }
}
