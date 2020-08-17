package io.paytouch.core.services.urbanairship

import scala.concurrent._

import akka.http.scaladsl.model._

import ch.qos.logback.classic.{ Level, Logger => ClassicLogger }

import com.typesafe.scalalogging.Logger

import org.specs2.concurrent.ExecutionEnv

import io.paytouch.core.clients.urbanairship.{ Error, PassLoader }
import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.entities.{ GiftCardPass, LoyaltyMembership }
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.enums.PassType._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.expansions.{ GiftCardPassExpansions, NoExpansions }
import io.paytouch.core.services.UrbanAirshipService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UrbanAirshipUpsertPassSpec extends UrbanAirshipServiceSpec {
  abstract class UrbanAirshipUpsertPassSpecContext[A] extends UrbanAirshipServiceSpecContext {
    val appleWalletTemplateId = "appleTemplateId"
    val androidPayTemplateId = "androidTemplateId"
    def expectedUpsertionData = passLoader.upsertionData(data)

    def data: A
    def expectedTemplateId: String
    def expectedExternalId: String
    implicit def passLoader: PassLoader[A]

    def setupUpdatePassNotFoundError() =
      walletClientMock.updatePass(any, any) returns Future.successful(
        Left(
          Error(
            uri = Uri.Empty,
            requestBody = "some-request-body-here",
            status = StatusCodes.NotFound,
            responseBody = "some-response-body-here",
            ex = None,
          ),
        ),
      )

    def setupUpdatePassSuccess() =
      walletClientMock.updatePass(any, any) returns Future.successful(Right(PassUpdateResponse("ticketIdResponse")))

    def setupCreatePassSuccess() =
      walletClientMock.createPass(any, any, any) returns Future.successful(Right(random[Pass]))

    def setupCreatePassError() =
      walletClientMock.createPass(any, any, any) returns Future.successful(
        Left(
          Error(
            uri = Uri.Empty,
            requestBody = "some-request-body-here",
            status = StatusCodes.BadGateway,
            responseBody = "some-response-body-here",
            ex = None,
          ),
        ),
      )

    def setupUpdatePassCallExpectation(passType: PassType) =
      there was one(walletClientMock).updatePass(
        expectedExternalId,
        expectedUpsertionData,
      )

    def setupCreatePassCallExpectation(passType: PassType) =
      there was one(walletClientMock).createPass(
        expectedTemplateId,
        expectedExternalId,
        expectedUpsertionData,
      )

    def setupCreatePassNoCallExpectation(passType: PassType) =
      there was no(walletClientMock).createPass(
        expectedTemplateId,
        expectedExternalId,
        expectedUpsertionData,
      )

    def assertCreatePass(passType: PassType) = {
      setupUpdatePassNotFoundError()
      setupCreatePassSuccess()

      service.upsertPass[A](expectedTemplateId, passType, data).await

      setupUpdatePassCallExpectation(passType)
      setupCreatePassCallExpectation(passType)
    }

    def assertCreatePassError(passType: PassType) = {
      implicit val ee = ExecutionEnv.fromGlobalExecutionContext

      setupUpdatePassNotFoundError()
      setupCreatePassError()

      service.upsertPass[A](expectedTemplateId, passType, data) must throwAn[RuntimeException].await

      setupUpdatePassCallExpectation(passType)
      setupCreatePassCallExpectation(passType)
    }

    def assertUpdatePass(passType: PassType) = {
      setupUpdatePassSuccess()

      service.upsertPass(expectedTemplateId, passType, data).await

      setupUpdatePassCallExpectation(passType)
      setupCreatePassNoCallExpectation(passType)
    }
  }

  trait LoyaltyMembershipFixtures extends UrbanAirshipUpsertPassSpecContext[LoyaltyMembership] {
    val customer = Factory.globalCustomer(Some(merchant)).create
    val loyaltyProgram = Factory
      .loyaltyProgram(
        merchant,
        locations = locations,
        appleWalletTemplateId = Some(appleWalletTemplateId),
        androidPayTemplateId = Some(androidPayTemplateId),
      )
      .create
    val loyaltyMembership =
      Factory.loyaltyMembership(customer, loyaltyProgram, points = Some(5)).create

    val data = random[LoyaltyMembership]
    implicit def passLoader = PassLoader.loyaltyMembershipLoader
  }

  trait GiftCardPassFixtures extends UrbanAirshipUpsertPassSpecContext[GiftCardPass] {
    val order = Factory.order(merchant).create
    val orderItem = Factory.orderItem(order).create
    val giftCardProduct = Factory.giftCardProduct(merchant).create
    val giftCard = Factory.giftCard(giftCardProduct).create
    val giftCardPass = Factory.giftCardPass(giftCard, orderItem).create
    val giftCardPassTransactionPayment1 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-50)).create
    val giftCardPassTransactionRefund = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(25)).create
    val giftCardPassTransactionPayment2 = Factory.giftCardPassTransaction(giftCardPass, totalAmount = Some(-20)).create

    val data = giftCardPassService
      .enrich(giftCardPass, giftCardPassService.defaultFilters)(GiftCardPassExpansions(withTransactions = true))
      .await
    implicit def passLoader = PassLoader.giftCardPassLoader
  }

  "UrbanAirshipService" in {

    "upsertPass with LoyaltyPassData" should {
      "with an Android pass type" should {
        "call WalletClient.updatePass if pass exist" in new LoyaltyMembershipFixtures {
          val expectedTemplateId = androidPayTemplateId
          val expectedExternalId = s"${Android.entryName}-${data.id}"
          assertUpdatePass(Android)
        }

        "call WalletClient.createPass if pass doesn't exist" in new LoyaltyMembershipFixtures {
          val expectedTemplateId = androidPayTemplateId
          val expectedExternalId = s"${Android.entryName}-${data.id}"
          assertCreatePass(Android)
        }

        "fails the future if there is an error creating the pass" in new LoyaltyMembershipFixtures {
          val expectedTemplateId = androidPayTemplateId
          val expectedExternalId = s"${Android.entryName}-${data.id}"
          assertCreatePassError(Android)
        }
      }

      "with an Ios pass type" should {
        "call WalletClient.updatePass if pass exist" in new LoyaltyMembershipFixtures {
          val expectedTemplateId = appleWalletTemplateId
          val expectedExternalId = s"${Ios.entryName}-${data.id}"
          assertUpdatePass(Ios)
        }

        "call WalletClient.createPass if pass doesn't exist" in new LoyaltyMembershipFixtures {
          val expectedTemplateId = appleWalletTemplateId
          val expectedExternalId = s"${Ios.entryName}-${data.id}"
          assertCreatePass(Ios)
        }

        "fails the future if there is an error creating the pass" in new LoyaltyMembershipFixtures {
          val expectedTemplateId = appleWalletTemplateId
          val expectedExternalId = s"${Ios.entryName}-${data.id}"
          assertCreatePassError(Ios)
        }
      }
    }

    "upsertPass with GiftCardPass" should {
      "prepare the upsertion data properly" in new GiftCardPassFixtures {
        val expectedTemplateId = "unused"
        val expectedExternalId = s"unused"

        val upsertionData = passLoader.upsertionData(data)
        upsertionData.fields.get("Last Spend") ==== Some(FieldValueUpdate(BigDecimal(20)))
      }

      "with an Android pass type" should {
        "call WalletClient.updatePass if pass exist" in new GiftCardPassFixtures {
          val expectedTemplateId = androidPayTemplateId
          val expectedExternalId = s"gift-${Android.entryName}-${data.id}"
          assertUpdatePass(Android)
        }

        "call WalletClient.createPass if pass doesn't exist" in new GiftCardPassFixtures {
          val expectedTemplateId = androidPayTemplateId
          val expectedExternalId = s"gift-${Android.entryName}-${data.id}"
          assertCreatePass(Android)
        }

        "fails the future if there is an error creating the pass" in new GiftCardPassFixtures {
          val expectedTemplateId = androidPayTemplateId
          val expectedExternalId = s"gift-${Android.entryName}-${data.id}"
          assertCreatePassError(Android)
        }
      }

      "with an Ios pass type" should {
        "call WalletClient.updatePass if pass exist" in new GiftCardPassFixtures {
          val expectedTemplateId = appleWalletTemplateId
          val expectedExternalId = s"gift-${Ios.entryName}-${data.id}"
          assertUpdatePass(Ios)
        }

        "call WalletClient.createPass if pass doesn't exist" in new GiftCardPassFixtures {
          val expectedTemplateId = appleWalletTemplateId
          val expectedExternalId = s"gift-${Ios.entryName}-${data.id}"
          assertCreatePass(Ios)
        }

        "fails the future if there is an error creating the pass" in new GiftCardPassFixtures {
          val expectedTemplateId = appleWalletTemplateId
          val expectedExternalId = s"gift-${Ios.entryName}-${data.id}"
          assertCreatePassError(Ios)
        }
      }
    }

    "prepareGiftCardTemplateData" should {
      "return GiftCardTemplateData" in new UrbanAirshipServiceSpecContext {
        val giftCardProduct = Factory.giftCardProduct(merchant).create
        val giftCard = Factory.giftCard(giftCardProduct, businessName = Some("My business")).create

        val expectedTemplateData = TemplateData.GiftCardTemplateData(
          merchantName = "My business",
          currentBalance = 0.$$$,
          originalBalance = 0.$$$,
          lastSpend = None,
          address = Some(s"address line 1 ${london.id}\n00100 Rome Lazio"),
          details = giftCard.templateDetails,
          androidImage = None,
          appleFullWidthImage = None,
          logoImage = None,
          phone = london.phoneNumber,
          website = london.website.get,
        )
        val giftCardEntity = giftCardService.enrich(giftCard, giftCardService.defaultFilters)(NoExpansions()).await
        val giftCardTemplateData = service.prepareGiftCardTemplateData(merchant.id, giftCardEntity).await.get

        giftCardTemplateData ==== expectedTemplateData
      }
    }
  }
}
