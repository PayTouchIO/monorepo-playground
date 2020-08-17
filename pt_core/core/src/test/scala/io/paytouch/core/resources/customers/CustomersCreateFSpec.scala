package io.paytouch.core.resources.customers

import java.util.UUID

import cats.implicits._

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ CustomerSource, MerchantSetupSteps }
import io.paytouch.core.utils.{ SetupStepsAssertions, FixtureDaoFactory => Factory }
import io.paytouch.core.services.UtilService

class CustomersCreateFSpec extends CustomersFSpec {
  abstract class CustomerCreateFSpecContext extends CustomerResourceFSpecContext with SetupStepsAssertions {
    def assertCreation(id: UUID, creation: CustomerMerchantUpsertion) =
      assertUpdate(id, creation)
  }

  "POST /v1/customers.create" in {
    "if request has valid token" in {
      "create a customer merchant and global customer without email" in new CustomerCreateFSpecContext {
        @scala.annotation.nowarn("msg=Auto-application")
        val creation =
          random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

        Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

          globalCustomerDao.findById(customerResponse.id).await.isDefined should beTrue
          assertCreation(customerResponse.id, creation)
          assertFullyExpandedCustomerResponse(customerResponse)

          assertSetupStepCompleted(merchant, MerchantSetupSteps.ImportCustomers)
        }
      }

      "create a customer merchant and enroll her in a loyalty program" should {
        "fail if email is not given" in new CustomerCreateFSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

          Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("EmailRequiredForLoyaltySignUp")
          }
        }
      }

      "create a customer merchant with email and enroll her in a loyalty program" should {
        "return 201" in new CustomerCreateFSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create

          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[CustomerMerchantUpsertion]
              .copy(email = Some(randomEmail), enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

          Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

            globalCustomerDao.findById(customerResponse.id).await.isDefined should beTrue
            assertCreation(customerResponse.id, creation)
            assertCustomerIsEnrolled(customerResponse.id, loyaltyProgram)

            val loyaltyMemberships = findCustomerMemberships(customerResponse.id, loyaltyProgram)
            assertFullyExpandedCustomerResponse(
              customerResponse,
              loyaltyProgramIds = Seq(loyaltyProgram.id),
              loyaltyMemberships = Some(loyaltyMemberships),
            )
          }
        }
      }

      "create a customer merchant and global customer with email" in new CustomerCreateFSpecContext {
        val newEmail = randomEmail

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

        Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

          globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail)
          assertCreation(customerResponse.id, creation)
          assertFullyExpandedCustomerResponse(customerResponse)
        }
      }

      "create a customer merchant with email and link it to existing global customer by email" in new CustomerCreateFSpecContext {
        val newEmail = randomEmail
        val globalCustomer = Factory.globalCustomerWithEmail(email = Some(newEmail)).create

        @scala.annotation.nowarn("msg=Auto-application")
        val creation = random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

        Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
          customerResponse.id ==== globalCustomer.id

          globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail)
          assertCreation(customerResponse.id, creation)
          assertFullyExpandedCustomerResponse(customerResponse)
        }
      }

      "create a customer merchant with email and link it to existing global customer by email even if billing details are not present" in new CustomerCreateFSpecContext {
        val newEmail = randomEmail
        val globalCustomer = Factory.globalCustomerWithEmail(email = Some(newEmail)).create

        @scala.annotation.nowarn("msg=Auto-application")
        val creation =
          random[CustomerMerchantUpsertion]
            .copy(
              email = newEmail.some,
              enrollInLoyaltyProgramId = None,
              billingDetails = None,
            )

        Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
          customerResponse.id ==== globalCustomer.id

          globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail)

          assertCreation(
            id = customerResponse.id,
            creation = creation,
          )

          assertFullyExpandedCustomerResponse(customerResponse)
        }
      }

      "create a customer with the correct source" should {
        "source = pt_dashboard" in new CustomerCreateFSpecContext {
          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.create", creation).addHeader(dashboardAuthorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.source ==== CustomerSource.PtDashboard
          }
        }

        "source = pt_register" in new CustomerCreateFSpecContext {
          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.create", creation).addHeader(registerAuthorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            customerResponse.source ==== CustomerSource.PtRegister
          }
        }
      }

      "if customer email is invalid" should {
        "return 400" in new CustomerCreateFSpecContext {
          @scala.annotation.nowarn("msg=Auto-application")
          val creation =
            random[CustomerMerchantUpsertion]
              .copy(email = Some("yadda"), enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.create", creation).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }
    }
  }
}
