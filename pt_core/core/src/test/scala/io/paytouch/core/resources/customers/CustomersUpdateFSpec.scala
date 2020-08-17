package io.paytouch.core.resources.customers

import java.time.LocalDate

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.validators.AddressValidator

@scala.annotation.nowarn("msg=Auto-application")
class CustomersUpdateFSpec extends CustomersFSpec {
  abstract class CustomerUpdateFSpecContext extends CustomerResourceFSpecContext {
    def assertSuccessfulUpdate(
        customerResponse: CustomerMerchant,
        update: CustomerMerchantUpsertion,
        globalCustomer: GlobalCustomerRecord,
        loyaltyProgram: Option[LoyaltyProgramRecord],
      ) = {
      customerResponse.id ==== globalCustomer.id

      assertUpdate(globalCustomer.id, update)

      val loyaltyMemberships =
        loyaltyProgram
          .map(findCustomerMemberships(customerResponse.id, _))
          .orElse(Some(Seq.empty))

      assertFullyExpandedCustomerResponse(
        customerResponse,
        loyaltyProgramIds = loyaltyProgram.map(_.id).toSeq,
        loyaltyMemberships = loyaltyMemberships,
      )

      if (loyaltyProgram.isDefined)
        assertCustomerIsEnrolled(globalCustomer.id, loyaltyProgram.get)
    }
  }

  "POST /v1/customers.update" in {
    "if request has valid token" in {
      "if global customer has no associated email" should {
        "update a customer merchant with email and add email to linked global customer" in new CustomerUpdateFSpecContext {
          val newEmail = randomEmail
          val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create

          val update = random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            assertSuccessfulUpdate(customerResponse, update, globalCustomer, None)

            globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(newEmail.toLowerCase)
          }
        }
      }

      "if global customer has already an email" should {
        "update a customer merchant with email and keep existing email in global customer" in new CustomerUpdateFSpecContext {
          val previousEmail = randomEmail
          val newEmail = randomEmail

          val globalCustomer =
            Factory.globalCustomerWithEmail(merchant = Some(merchant), email = Some(previousEmail)).create

          val update = random[CustomerMerchantUpsertion].copy(email = Some(newEmail), enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
            assertSuccessfulUpdate(customerResponse, update, globalCustomer, None)

            globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(previousEmail)
          }
        }
      }

      "reset a customer dob and anniversary" in new CustomerUpdateFSpecContext {
        val date = LocalDate.of(1987, 11, 22)

        val globalCustomer = Factory
          .globalCustomerWithEmail(
            merchant = Some(merchant),
            email = Some(email),
            dob = Some(date),
            anniversary = Some(date),
          )
          .create

        val update = random[CustomerMerchantUpsertion].copy(
          email = None,
          enrollInLoyaltyProgramId = None,
          dob = ResettableLocalDate.reset,
          anniversary = ResettableLocalDate.reset,
        )

        Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data
          assertSuccessfulUpdate(customerResponse, update, globalCustomer, None)

          globalCustomerDao.findById(customerResponse.id).await.get.email ==== Some(email)
          customerResponse.dob should beNone
          customerResponse.anniversary should beNone
        }
      }

      "if merchant has not been associated to the global customer" should {
        "reject request" in new CustomerUpdateFSpecContext {
          val globalCustomer = Factory.globalCustomer().create

          val update = random[CustomerMerchantUpsertion].copy(email = Some(randomEmail))

          Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if request specifies a loyalty program id" should {
        "if customer already has an email" should {
          "update a customer merchant and enroll her in loyalty program" in new CustomerUpdateFSpecContext {
            val loyaltyProgram =
              Factory
                .loyaltyProgram(merchant)
                .create

            val globalCustomer =
              Factory
                .globalCustomerWithEmail(merchant = Some(merchant), email = Some(randomEmail))
                .create

            val update =
              random[CustomerMerchantUpsertion]
                .copy(email = None, enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

            import AddressValidator._
            AddressValidator.validated(update.address) ==== update.address.good

            Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

              assertSuccessfulUpdate(
                customerResponse,
                update,
                globalCustomer,
                Some(loyaltyProgram),
              )
            }
          }
        }

        "if customer email is being set" should {
          "update a customer merchant and enroll her in loyalty program" in new CustomerUpdateFSpecContext {
            val loyaltyProgram =
              Factory
                .loyaltyProgram(merchant)
                .create

            val globalCustomer =
              Factory
                .globalCustomerWithEmail(merchant = Some(merchant), email = None)
                .create

            val update =
              random[CustomerMerchantUpsertion]
                .copy(
                  email = Some(randomEmail),
                  enrollInLoyaltyProgramId = Some(loyaltyProgram.id),
                )

            Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {

              assertStatusOK()

              val customerResponse = responseAs[ApiResponse[CustomerMerchant]].data

              assertSuccessfulUpdate(
                customerResponse,
                update,
                globalCustomer,
                Some(loyaltyProgram),
              )
            }
          }
        }

        "if customer has no email  and it isn't being set" should {
          "return 400" in new CustomerUpdateFSpecContext {
            val loyaltyProgram = Factory.loyaltyProgram(merchant).create
            val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create
            val update =
              random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = Some(loyaltyProgram.id))

            Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("EmailRequiredForLoyaltySignUp")
            }
          }
        }
      }

      "if customer email is invalid" should {
        "return 400" in new CustomerUpdateFSpecContext {
          val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(merchant), email = None).create
          val update =
            random[CustomerMerchantUpsertion].copy(email = Some("yadda"), enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCodesAtLeastOnce("InvalidEmail")
          }
        }
      }

      "if customer belongs to another merchant" should {
        "return 404" in new CustomerUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val globalCustomer = Factory.globalCustomerWithEmail(merchant = Some(competitor), email = None).create
          val update =
            random[CustomerMerchantUpsertion].copy(email = None, enrollInLoyaltyProgramId = None)

          Post(s"/v1/customers.update?customer_id=${globalCustomer.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertErrorCodesAtLeastOnce("NonAccessibleCustomerIds")
          }
        }
      }
    }
  }
}
