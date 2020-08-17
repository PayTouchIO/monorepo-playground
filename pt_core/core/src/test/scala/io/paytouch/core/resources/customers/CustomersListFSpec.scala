package io.paytouch.core.resources.customers

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.CustomerSource
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CustomersListFSpec extends CustomersFSpec {

  "GET /v1/customers.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all customers sorted by last name and first name" in new CustomerResourceFSpecContext {
          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
            .create
          val customer4 = Factory
            .globalCustomer(firstName = Some("Marco"), lastName = Some("Carrot"))
            .create
          val customerMerchant4 = Factory
            .customerMerchant(merchant = merchant, customer = customer4, source = Some(CustomerSource.UberEats))
            .create

          Get("/v1/customers.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id, customer2.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get)
            assertCustomerResponse(customers.data.find(_.id == customer2.id).get)
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get)
          }
        }
      }

      "with filter source" should {
        "return a paginated list of all customers filtered by source" in new CustomerResourceFSpecContext {
          val customer1 = Factory
            .globalCustomer(lastName = Some("A"))
            .create
          val customerMerchant1 = Factory
            .customerMerchant(merchant = merchant, customer = customer1, source = Some(CustomerSource.PtRegister))
            .create
          val customer2 = Factory
            .globalCustomer(lastName = Some("B"))
            .create
          val customerMerchant2 = Factory
            .customerMerchant(merchant = merchant, customer = customer2, source = Some(CustomerSource.PtDashboard))
            .create
          val customer3 = Factory
            .globalCustomer(lastName = Some("C"))
            .create
          val customerMerchant3 = Factory
            .customerMerchant(merchant = merchant, customer = customer3, source = Some(CustomerSource.UberEats))
            .create

          Get("/v1/customers.list?source[]=pt_register,uber_eats").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id)
          }
        }
      }

      "with filter location_id" should {

        "if the location id belongs to the merchant" should {

          "return a paginated list of all customers filtered by location" in new CustomerResourceFSpecContext {
            val customer1 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
              .create
            val customer2 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
              .create
            val customer3 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
              .create

            val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create

            Factory.customerLocation(customer1, rome).create
            Factory.customerLocation(customer1, london).create
            Factory.customerLocation(customer2, london).create
            Factory.customerLocation(customer3, rome).create
            Factory.customerLocation(customer3, deletedLocation).create

            Get(s"/v1/customers.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
              customers.data.map(_.id) ==== Seq(customer1.id, customer3.id)
              assertCustomerResponse(customers.data.find(_.id == customer1.id).get)
              assertCustomerResponse(customers.data.find(_.id == customer3.id).get)
            }
          }
        }

        "if the location id does not belong to the merchant" should {

          "return no items" in new CustomerResourceFSpecContext {
            val competitor = Factory.merchant.create
            val newYork = Factory.location(competitor).create

            val customer1 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
              .create
            val customer2 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
              .create
            val customer3 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
              .create

            Factory.customerLocation(customer1, newYork).create
            Factory.customerLocation(customer2, newYork).create
            Factory.customerLocation(customer3, newYork).create

            Get(s"/v1/customers.list?location_id=${newYork.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
              customers.data.map(_.id) ==== Seq.empty
            }
          }
        }
      }

      "with filter group_id" should {

        "if the group id belongs to the merchant" should {
          "return a paginated list of all customers in a group" in new CustomerResourceFSpecContext {
            val customer1 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
              .create
            val customer2 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
              .create
            val customer3 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
              .create

            val goldenGroup = Factory.group(merchant).create
            val silverGroup = Factory.group(merchant).create

            Factory.customerGroup(customer1, goldenGroup).create
            Factory.customerGroup(customer1, silverGroup).create
            Factory.customerGroup(customer2, silverGroup).create

            Get(s"/v1/customers.list?group_id=${silverGroup.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
              customers.data.map(_.id) ==== Seq(customer1.id, customer2.id)
              assertCustomerResponse(customers.data.find(_.id == customer1.id).get)
              assertCustomerResponse(customers.data.find(_.id == customer2.id).get)
            }
          }
        }

        "if the group id does not belong to the merchant" should {
          "return no results" in new CustomerResourceFSpecContext {
            val customer1 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
              .create
            val customer2 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
              .create
            val customer3 = Factory
              .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
              .create

            val competitor = Factory.merchant.create
            val competitorGroup = Factory.group(competitor).create

            Factory.customerGroup(customer1, competitorGroup).create
            Factory.customerGroup(customer2, competitorGroup).create

            Get(s"/v1/customers.list?group_id=${competitorGroup.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
              customers.data.map(_.id) ==== Seq.empty
            }
          }
        }
      }

      "with filter query" should {
        "return a paginated list of all customers filtered by query" in new CustomerResourceFSpecContext {
          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
            .create

          Get("/v1/customers.list?q=Barack").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get)
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get)
          }
        }

        "return a paginated list of all customers filtered by query when either the name or last name are missing" in new CustomerResourceFSpecContext {
          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = None, lastName = Some("Barack"))
            .create

          val customer4 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco Barack"), lastName = None)
            .create

          Get("/v1/customers.list?q=Barack").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id, customer4.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get)
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get)
            assertCustomerResponse(customers.data.find(_.id == customer4.id).get)
          }
        }
      }

      "filtered by updated_since date-time" should {
        "return a paginated list of all customers filtered by updated_since" in new CustomerResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val customer1 =
            Factory.globalCustomer(merchant = Some(merchant), overrideNow = Some(now.minusDays(1))).create
          val customer2 = Factory.globalCustomer(merchant = Some(merchant), overrideNow = Some(now)).create
          val customer3 = Factory.globalCustomer(merchant = Some(merchant), overrideNow = Some(now.plusDays(1))).create

          Get("/v1/customers.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) should containTheSameElementsAs(Seq(customer2.id, customer3.id))
            assertCustomerResponse(customers.data.find(_.id == customer2.id).get)
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get)
          }
        }
      }

      "with expand[]=visits" should {
        "return a paginated list of all customers with expanded visits aggregates" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
            .create

          Factory.customerLocation(customer1, rome, totalVisits = Some(5)).create
          Factory.customerLocation(customer1, london, totalVisits = Some(10)).create

          Factory.customerLocation(customer2, london, totalVisits = Some(3)).create

          Factory.customerLocation(customer3, rome, totalVisits = Some(1)).create

          Factory.customerLocation(customer3, newYork, totalVisits = Some(1)).create

          Get(s"/v1/customers.list?expand[]=visits").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id, customer2.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get, totalVisits = Some(15))
            assertCustomerResponse(customers.data.find(_.id == customer2.id).get, totalVisits = Some(3))
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get, totalVisits = Some(1))
          }
        }
      }

      "with expand[]=visits and filter location_id" should {
        "return a paginated list of all customers with expanded visits aggregates filtered by location" in new CustomerResourceFSpecContext {
          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
            .create

          Factory.customerLocation(customer1, rome, totalVisits = Some(5)).create
          Factory.customerLocation(customer1, london, totalVisits = Some(10)).create
          Factory.customerLocation(customer2, london, totalVisits = Some(3)).create
          Factory.customerLocation(customer3, rome, totalVisits = Some(1)).create

          Get(s"/v1/customers.list?expand[]=visits&location_id=${rome.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get, totalVisits = Some(5))
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get, totalVisits = Some(1))
          }
        }
      }

      "with expand[]=locations" should {
        "return a paginated list of all customers with expanded locations" in new CustomerResourceFSpecContext {
          val customer = Factory.globalCustomer(merchant = Some(merchant)).create

          val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
          Factory.customerLocation(customer, london).create
          Factory.customerLocation(customer, deletedLocation).create

          Get("/v1/customers.list?expand[]=locations").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer.id)
            assertCustomerResponse(customers.data.find(_.id == customer.id).get, locationIds = Seq(london.id))
          }
        }
      }

      "with expand[]=loyalty_programs" should {
        "return a paginated list of all customers with expanded loyalty programs" in new CustomerResourceFSpecContext {
          val customer = Factory.globalCustomer(merchant = Some(merchant)).create
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          Factory.loyaltyMembership(customer, loyaltyProgram).create

          Get("/v1/customers.list?expand[]=loyalty_programs").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer.id)
            assertCustomerResponse(
              customers.data.find(_.id == customer.id).get,
              loyaltyProgramIds = Some(Seq(loyaltyProgram.id)),
            )
          }
        }
      }

      "with expand[]=loyalty_memberships" should {
        "return a paginated list of all customers with expanded loyalty statuses" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant, zoneId = Some("America/New_York")).create
          val customer = Factory.globalCustomer(Some(merchant)).create
          val loyaltyProgram = Factory.loyaltyProgram(merchant, pointsToReward = Some(100)).create

          val loyaltyMembership =
            Factory.loyaltyMembership(customer, loyaltyProgram, points = Some(10)).create
          val expectedLoyaltyMembership =
            LoyaltyMembership(
              id = loyaltyMembership.id,
              customerId = customer.id,
              loyaltyProgramId = loyaltyProgram.id,
              lookupId = loyaltyMembership.lookupId,
              points = 10,
              pointsToNextReward = 90,
              passPublicUrls = PassUrls(),
              customerOptInAt = None,
              merchantOptInAt = None,
              enrolled = false,
              visits = 5,
              totalSpend = 90.$$$,
            )

          Factory.customerLocation(customer, rome, totalVisits = Some(5), totalSpend = Some(90)).create

          Get("/v1/customers.list?expand[]=loyalty_memberships").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val entities = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]].data
            entities.map(_.id) ==== Seq(customer.id)
            assertCustomerResponse(entities.head, loyaltyMemberships = Some(Seq(expectedLoyaltyMembership)))
          }
        }
      }

      "with expand[]=spend" should {
        "return a paginated list of all customers with expanded spend aggregates" in new CustomerResourceFSpecContext {
          val newYork = Factory.location(merchant).create

          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
            .create

          Factory.customerLocation(customer1, rome, totalSpend = Some(5)).create
          Factory.customerLocation(customer1, london, totalSpend = Some(10)).create

          Factory.customerLocation(customer2, london, totalSpend = Some(3)).create

          Factory.customerLocation(customer3, rome, totalSpend = Some(1)).create

          Factory.customerLocation(customer3, newYork, totalSpend = Some(1)).create

          Get(s"/v1/customers.list?expand[]=spend").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id, customer2.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get, totalSpend = Some(Seq(15.$$$)))
            assertCustomerResponse(customers.data.find(_.id == customer2.id).get, totalSpend = Some(Seq(3.$$$)))
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get, totalSpend = Some(Seq(1.$$$)))
          }
        }
      }

      "with expand[]=spend and filter location_id" should {
        "return a paginated list of all customers with expanded spend aggregates filtered by location" in new CustomerResourceFSpecContext {
          val customer1 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Barack"))
            .create
          val customer2 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Andrew"), lastName = Some("Smith"))
            .create
          val customer3 = Factory
            .globalCustomer(merchant = Some(merchant), firstName = Some("Marco"), lastName = Some("Barack"))
            .create

          Factory.customerLocation(customer1, rome, totalSpend = Some(5)).create
          Factory.customerLocation(customer1, london, totalSpend = Some(10)).create
          Factory.customerLocation(customer2, london, totalSpend = Some(3)).create
          Factory.customerLocation(customer3, rome, totalSpend = Some(1)).create

          Get(s"/v1/customers.list?expand[]=spend&location_id=${rome.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customers = responseAs[PaginatedApiResponse[Seq[CustomerMerchant]]]
            customers.data.map(_.id) ==== Seq(customer1.id, customer3.id)
            assertCustomerResponse(customers.data.find(_.id == customer1.id).get, totalSpend = Some(Seq(5.$$$)))
            assertCustomerResponse(customers.data.find(_.id == customer3.id).get, totalSpend = Some(Seq(1.$$$)))
          }
        }
      }
    }
  }
}
