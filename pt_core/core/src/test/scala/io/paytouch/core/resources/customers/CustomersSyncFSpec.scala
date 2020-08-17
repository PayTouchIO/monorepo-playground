package io.paytouch.core.resources.customers

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.CustomerSource
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

@scala.annotation.nowarn("msg=Auto-application")
class CustomersSyncFSpec extends CustomersFSpec {
  abstract class CustomerSyncFSpecContext extends CustomerResourceFSpecContext

  "if we are creating a new customer" in {
    "POST /v1/customers.sync" in {
      "if request has valid token" in {
        "create a customer merchant and global customer without email" in new CustomerSyncFSpecContext {
          val customerId = UUID.randomUUID
          val upsertion = random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.sync?customer_id=$customerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

            globalCustomerDao.findById(customerResponse.id).await.isDefined should beTrue
            assertUpdate(customerResponse.id, upsertion)
            assertFullyExpandedCustomerResponse(customerResponse)
          }
        }

        "create a customer merchant and enroll her in a loyalty program" in new CustomerSyncFSpecContext {
          val customerId = UUID.randomUUID
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          val upsertion =
            random[CustomerMerchantUpsertion]
              .copy(email = Some(randomEmail), enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

          Post(s"/v1/customers.sync?customer_id=$customerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

            globalCustomerDao.findById(customerResponse.id).await.isDefined should beTrue
            assertUpdate(customerResponse.id, upsertion)
            assertCustomerIsEnrolled(customerResponse.id, loyaltyProgram)

            val loyaltyMemberships = findCustomerMemberships(customerResponse.id, loyaltyProgram)
            assertFullyExpandedCustomerResponse(
              customerResponse,
              loyaltyProgramIds = Seq(loyaltyProgram.id),
              loyaltyMemberships = Some(loyaltyMemberships),
            )
          }
        }

        "create a customer merchant and global customer with email" in new CustomerSyncFSpecContext {
          val customerId = UUID.randomUUID
          val newEmail = randomEmail
          val upsertion =
            random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.sync?customer_id=$customerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

            globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail)
            assertUpdate(customerResponse.id, upsertion)
            assertFullyExpandedCustomerResponse(customerResponse)
          }
        }

        "create a customer merchant with email and link it to existing global customer by email" in new CustomerSyncFSpecContext {
          val customerId = UUID.randomUUID
          val newEmail = randomEmail
          val globalCustomer = Factory.globalCustomerWithEmail(email = Some(newEmail)).create

          val upsertion =
            random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.sync?customer_id=$customerId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

            customerResponse.id ==== globalCustomer.id
            customerId !=== globalCustomer.id

            globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail)
            assertUpdate(customerResponse.id, upsertion)
            assertFullyExpandedCustomerResponse(customerResponse)
          }
        }

        "create a customer with the correct source" should {
          "source = pt_dashboard" in new CustomerSyncFSpecContext {
            val customerId = UUID.randomUUID
            val creation =
              random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

            Post(s"/v1/customers.sync?customer_id=$customerId", creation)
              .addHeader(dashboardAuthorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              customerResponse.source ==== CustomerSource.PtDashboard
            }
          }

          "source = pt_register" in new CustomerSyncFSpecContext {
            val customerId = UUID.randomUUID
            val creation =
              random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

            Post(s"/v1/customers.sync?customer_id=$customerId", creation)
              .addHeader(registerAuthorizationHeader) ~> routes ~> check {
              assertStatusCreated()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              customerResponse.source ==== CustomerSource.PtRegister
            }
          }
        }
      }
    }
  }

  "if we are updating an existing customer" in {
    "POST /v1/customers.sync" in {
      "if request has valid token" in {
        "if global customer has no associated email" should {
          "update a customer merchant with email and add email to linked global customer" in new CustomerSyncFSpecContext {
            val newEmail = randomEmail
            val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create
            Factory.customerLocation(globalCustomer, rome, totalVisits = Some(0), totalSpend = Some(0)).create

            val update =
              random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

            Post(s"/v1/customers.sync?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              customerResponse.id ==== globalCustomer.id

              globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail)
              assertUpdate(globalCustomer.id, update)
              assertFullyExpandedCustomerResponse(customerResponse, locationIds = Some(Seq(rome.id)))
            }
          }
        }

        "if global customer has already an email" should {
          "update a customer merchant with email and keep existing email in global customer" in new CustomerSyncFSpecContext {
            val previousEmail = randomEmail
            val newEmail = randomEmail

            val globalCustomer =
              Factory.globalCustomerWithEmail(merchant = Some(merchant), email = Some(previousEmail)).create

            val update =
              random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

            Post(s"/v1/customers.sync?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              customerResponse.id ==== globalCustomer.id

              globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(previousEmail)
              assertUpdate(globalCustomer.id, update)
              assertFullyExpandedCustomerResponse(customerResponse)
            }
          }
        }

        "if request specifies a loyalty program id" should {
          "update a customer merchant and enroll her in loyalty program without signup reward" in new CustomerSyncFSpecContext {
            val loyaltyProgram = Factory.loyaltyProgram(merchant, signupRewardEnabled = Some(false)).create
            val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create
            val update =
              random[CustomerMerchantUpsertion]
                .copy(email = Some(randomEmail), enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

            Post(s"/v1/customers.sync?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              customerResponse.id ==== globalCustomer.id

              assertUpdate(globalCustomer.id, update)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram, expectedBalance = Some(0))

              val loyaltyMemberships = findCustomerMemberships(globalCustomer.id, loyaltyProgram)
              assertFullyExpandedCustomerResponse(
                customerResponse,
                loyaltyProgramIds = Seq(loyaltyProgram.id),
                loyaltyMemberships = Some(loyaltyMemberships),
              )
            }
          }

          "update a customer merchant and enroll her in loyalty program with signup reward" in new CustomerSyncFSpecContext {
            val loyaltyProgram =
              Factory.loyaltyProgram(merchant, signupRewardEnabled = Some(true), signupRewardPoints = Some(100)).create
            val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create
            val update =
              random[CustomerMerchantUpsertion]
                .copy(email = Some(randomEmail), enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

            Post(s"/v1/customers.sync?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
              customerResponse.id ==== globalCustomer.id

              assertUpdate(globalCustomer.id, update)
              assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram, expectedBalance = Some(100))

              val loyaltyMemberships = findCustomerMemberships(globalCustomer.id, loyaltyProgram)
              assertFullyExpandedCustomerResponse(
                customerResponse,
                loyaltyProgramIds = Seq(loyaltyProgram.id),
                loyaltyMemberships = Some(loyaltyMemberships),
              )
            }
          }
        }
      }
    }
  }
}
