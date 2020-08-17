package io.paytouch.core.resources.customers

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CustomersGetFSpec extends CustomersFSpec {

  "GET /v1/customers.get?customer_id=<customer_id>" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return merchant specific customer merchant data" in new CustomerResourceFSpecContext {
          val customer = Factory.globalCustomer(Some(merchant)).create

          Get(s"/v1/customers.get?customer_id=${customer.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== customer.id
            assertCustomerResponse(customerResponse)
          }
        }
      }

      "with expand[]=spend" should {
        "return data with total spend aggregated over locations accessible to user" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customer = Factory.globalCustomer(Some(merchant)).create

          Factory.customerLocation(customer, rome, totalSpend = Some(5)).create
          Factory.customerLocation(customer, london, totalSpend = Some(10)).create
          Factory.customerLocation(customer, newYork, totalSpend = Some(1)).create

          Get(s"/v1/customers.get?customer_id=${customer.id}&expand[]=spend")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== customer.id
            assertCustomerResponse(customerResponse, totalSpend = Some(Seq(15.$$$)))
          }
        }
      }

      "with expand[]=visits" should {
        "return data with total visits aggregated over locations accessible to user" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customer = Factory.globalCustomer(Some(merchant)).create

          Factory.customerLocation(customer, rome, totalVisits = Some(5)).create
          Factory.customerLocation(customer, london, totalVisits = Some(10)).create
          Factory.customerLocation(customer, newYork, totalVisits = Some(1)).create

          Get(s"/v1/customers.get?customer_id=${customer.id}&expand[]=visits")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== customer.id
            assertCustomerResponse(customerResponse, totalVisits = Some(15))
          }
        }
      }

      "with expand[]=avg_tips" should {
        "return data with average tips aggregated over locations accessible to user" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customer = Factory.globalCustomer().create
          val customerMerchant = Factory.customerMerchant(merchant, customer).create

          Factory.customerLocation(customer, rome).create
          Factory.customerLocation(customer, newYork).create

          Factory
            .order(merchant, location = Some(rome), customer = Some(customerMerchant), tipAmount = Some(3.0))
            .create
          Factory
            .order(merchant, location = Some(rome), customer = Some(customerMerchant), tipAmount = Some(6.0))
            .create
          Factory
            .order(merchant, location = Some(newYork), customer = Some(customerMerchant), tipAmount = Some(9.0))
            .create

          Get(s"/v1/customers.get?customer_id=${customer.id}&expand[]=avg_tips")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== customer.id
            assertCustomerResponse(customerResponse, avgTips = Some(Seq(4.5.$$$)))
          }
        }
      }

      "with expand[]=loyalty_memberships" should {
        "return data with loyalty statuses for currently joined loyalty programs" in new CustomerResourceFSpecContext {
          val globalCustomer = Factory.globalCustomer().create

          val competitor = Factory.merchant.create
          val customerCompetitor = Factory.customerMerchant(competitor, globalCustomer).create
          val loyaltyProgramCompetitor = Factory.loyaltyProgram(competitor).create
          val loyaltyMembershipCompetitor =
            Factory.loyaltyMembership(globalCustomer, loyaltyProgramCompetitor, points = Some(10)).create

          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customerMerchant = Factory.customerMerchant(merchant, globalCustomer).create
          val loyaltyProgram = Factory.loyaltyProgram(merchant, pointsToReward = Some(300)).create

          val loyaltyMembership =
            Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(10)).create
          val expectedLoyaltyMembership =
            LoyaltyMembership(
              id = loyaltyMembership.id,
              customerId = globalCustomer.id,
              loyaltyProgramId = loyaltyProgram.id,
              lookupId = loyaltyMembership.lookupId,
              points = 10,
              pointsToNextReward = 290,
              passPublicUrls = PassUrls(),
              customerOptInAt = None,
              merchantOptInAt = None,
              enrolled = false,
              visits = 5,
              totalSpend = 9.$$$,
            )

          Factory.customerLocation(globalCustomer, rome, totalVisits = Some(5), totalSpend = Some(9)).create

          Get(s"/v1/customers.get?customer_id=${globalCustomer.id}&expand[]=loyalty_memberships")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== globalCustomer.id
            assertCustomerResponse(customerResponse, loyaltyMemberships = Some(Seq(expectedLoyaltyMembership)))
          }
        }
      }

      "with expand[]=loyalty_memberships and filter by loyalty program id" should {
        "return data with loyalty statuses for currently joined loyalty programs filtered by loyalty program id" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customer = Factory.globalCustomer(Some(merchant)).create
          val loyaltyProgramA = Factory.loyaltyProgram(merchant, pointsToReward = Some(200)).create
          val loyaltyMembershipA =
            Factory.loyaltyMembership(customer, loyaltyProgramA, points = Some(10)).create

          val loyaltyProgramB = Factory.loyaltyProgram(merchant).create
          val loyaltyMembershipB =
            Factory.loyaltyMembership(customer, loyaltyProgramB, points = Some(5)).create

          val expectedLoyaltyMembership =
            LoyaltyMembership(
              id = loyaltyMembershipA.id,
              customerId = customer.id,
              loyaltyProgramId = loyaltyProgramA.id,
              lookupId = loyaltyMembershipA.lookupId,
              points = 10,
              pointsToNextReward = 190,
              passPublicUrls = PassUrls(),
              customerOptInAt = None,
              merchantOptInAt = None,
              enrolled = false,
              visits = 5,
              totalSpend = 9.$$$,
            )

          Factory.customerLocation(customer, rome, totalVisits = Some(5), totalSpend = Some(9)).create

          Get(
            s"/v1/customers.get?customer_id=${customer.id}&expand[]=loyalty_memberships&loyalty_program_id=${loyaltyProgramA.id}",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.id ==== customer.id
            assertCustomerResponse(customerResponse, loyaltyMemberships = Some(Seq(expectedLoyaltyMembership)))
          }
        }
      }

      "with expand[]=loyalty_memberships and filter by updated_since" should {
        "return data with loyalty statuses for currently joined loyalty programs changed after the updated_since" in new CustomerResourceFSpecContext {
          val old = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customer = Factory.globalCustomer(Some(merchant)).create
          val loyaltyProgramA = Factory.loyaltyProgram(merchant, pointsToReward = Some(300)).create

          val loyaltyMembershipA =
            Factory.loyaltyMembership(customer, loyaltyProgramA, points = Some(10), overrideNow = Some(old)).create

          val updated = ZonedDateTime.parse("2015-12-05T20:15:30Z")
          val loyaltyProgramB = Factory.loyaltyProgram(merchant, pointsToReward = Some(300)).create
          val loyaltyMembershipB =
            Factory.loyaltyMembership(customer, loyaltyProgramB, points = Some(5), overrideNow = Some(updated)).create

          val expectedLoyaltyMembership =
            LoyaltyMembership(
              id = loyaltyMembershipB.id,
              customerId = customer.id,
              loyaltyProgramId = loyaltyProgramB.id,
              lookupId = loyaltyMembershipB.lookupId,
              points = 5,
              pointsToNextReward = 295,
              passPublicUrls = PassUrls(),
              customerOptInAt = None,
              merchantOptInAt = None,
              enrolled = false,
              visits = 5,
              totalSpend = 9.$$$,
            )

          Factory.customerLocation(customer, newYork, totalVisits = Some(5), totalSpend = Some(9)).create

          Get(s"/v1/customers.get?customer_id=${customer.id}&updated_since=2015-12-04&expand[]=loyalty_memberships")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.loyaltyMemberships ==== Some(Seq(expectedLoyaltyMembership))
          }
        }
      }

    }
  }
}
